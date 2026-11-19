package tn.oliveconnect.orchard;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrchardIntegrationTest extends AbstractIntegrationTest {

    static final String BODY = """
        {"name":"North Orchard","areaHectares":4.2,"oliveVariety":"Chemlali",
         "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}""";

    String create(String token) throws Exception {
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    @Test
    void create_returnsContractShape_lonLatOrderKept() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maturityStatus").value("UNKNOWN"))
                .andExpect(jsonPath("$.location.coordinates[0]").value(10.76))   // longitude d'abord
                .andExpect(jsonPath("$.location.coordinates[1]").value(34.73))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "farmerId", "name", "areaHectares", "oliveVariety", "location",
                "maturityStatus", "publicVisibility", "createdAt", "updatedAt");
    }

    @Test
    void farmerA_cannotReadFarmerB_orchard_404() throws Exception {
        String farmerA = bearer(Role.FARMER, "a@example.test");
        String farmerB = bearer(Role.FARMER, "b@example.test");
        String idOfA = create(farmerA);
        mvc.perform(get("/api/v1/orchards/" + idOfA).header("Authorization", farmerB))
           .andExpect(status().isNotFound())
           .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mvc.perform(patch("/api/v1/orchards/" + idOfA).header("Authorization", farmerB)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hack\"}"))
           .andExpect(status().isNotFound());
    }

    @Test
    void manager_canReadAnyOrchard_andSetMaturity() throws Exception {
        String id = create(bearer(Role.FARMER, "a@example.test"));
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        mvc.perform(patch("/api/v1/orchards/" + id).header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maturityStatus\":\"OPTIMAL\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.maturityStatus").value("OPTIMAL"));
    }

    @Test
    void farmer_cannotSetMaturity_403() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String id = create(farmer);
        mvc.perform(patch("/api/v1/orchards/" + id).header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"maturityStatus\":\"OPTIMAL\"}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void invalidArea_andCoordinates_return400() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("4.2", "0")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.areaHectares").exists());
        mvc.perform(post("/api/v1/orchards").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("34.73", "95")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors['location.coordinates']").exists());
    }

    @Test
    void mine_returnsOnlyOwnOrchards_inPageResponse() throws Exception {
        String a = bearer(Role.FARMER, "a@example.test");
        create(a); create(bearer(Role.FARMER, "b@example.test"));
        String json = mvc.perform(get("/api/v1/orchards/mine").header("Authorization", a))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "content", "page", "size", "totalElements", "totalPages");
    }
}
