package tn.oliveconnect.harvest;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class HarvestRequestIntegrationTest extends AbstractIntegrationTest {

    String farmer;
    String orchardId;

    void setup() throws Exception {
        farmer = bearer(Role.FARMER, "a@example.test");
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                          {"name":"North Orchard","areaHectares":4.2,"oliveVariety":"Chemlali",
                           "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}"""))
                .andReturn().getResponse().getContentAsString();
        orchardId = JsonPath.read(json, "$.id");
    }

    String body(String qty, String type, String reason) {
        return """
            {"orchardId":"%s","type":"%s","preferredDate":"%s","estimatedQuantityKg":%s,"urgencyReason":%s,
             "farmerId":"6741a0c2e4b0a1b2c3d4e5f6","status":"COMPLETED"}"""
            .formatted(orchardId, type, LocalDate.now().plusDays(3), qty, reason == null ? "null" : "\"" + reason + "\"");
    }

    @Test
    void create_201_submitted_farmerIdFromToken_bodyFieldsIgnored() throws Exception {
        setup();
        String json = mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(body("2500", "URGENT", "Maturité optimale")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))          // pas COMPLETED
                .andExpect(jsonPath("$.orchardName").value("North Orchard"))
                .andExpect(jsonPath("$.actualQuantityKg").isEmpty())
                .andReturn().getResponse().getContentAsString();
        String farmerId = JsonPath.read(json, "$.farmerId");
        org.assertj.core.api.Assertions.assertThat(farmerId).isNotEqualTo("6741a0c2e4b0a1b2c3d4e5f6");
        assertExactKeys(json, "id", "farmerId", "orchardId", "orchardName", "type", "urgencyReason", "status",
                "requestedAt", "preferredDate", "estimatedQuantityKg", "actualQuantityKg", "reviewedBy", "reviewNote",
                "scheduledTourId", "scheduledAt", "completedAt", "createdAt", "updatedAt");
    }

    @Test
    void quantityZeroOrNegative_400() throws Exception {
        setup();
        for (String q : new String[]{"0", "-5"}) {
            mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                            .contentType(MediaType.APPLICATION_JSON).content(body(q, "PLANNED", null)))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.fieldErrors.estimatedQuantityKg").value("Doit être > 0"));
        }
    }

    @Test
    void urgentWithoutReason_400() throws Exception {
        setup();
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "URGENT", null)))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.urgencyReason").exists());
    }

    @Test
    void orchardOfAnotherFarmer_404() throws Exception {
        setup();
        String other = bearer(Role.FARMER, "b@example.test");
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", other)
                        .contentType(MediaType.APPLICATION_JSON).content(body("100", "PLANNED", null)))
           .andExpect(status().isNotFound());
    }

    @Test
    void managerQueue_filtersByStatus_farmerGets403() throws Exception {
        setup();
        mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmer)
                .contentType(MediaType.APPLICATION_JSON).content(body("100", "PLANNED", null)));
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        mvc.perform(get("/api/v1/harvest-requests?status=SUBMITTED").header("Authorization", manager))
           .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/harvest-requests?status=FOO").header("Authorization", manager))
           .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/harvest-requests").header("Authorization", farmer))
           .andExpect(status().isForbidden());
    }
}
