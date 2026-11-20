package tn.oliveconnect.alert;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class FieldAlertIntegrationTest extends AbstractIntegrationTest {

    String createAlert(String farmer, String orchardId) throws Exception {
        String body = """
            {"orchardId":"%s","type":"PEST","description":"Activité de ravageurs",
             "location":{"type":"Point","coordinates":[10.76,34.73]},"priority":"HIGH"}""".formatted(orchardId);
        String json = mvc.perform(post("/api/v1/alerts").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "farmerId", "orchardId", "type", "description", "location",
                "priority", "status", "createdAt", "acknowledgedAt", "resolvedAt", "resolutionNote");
        return JsonPath.read(json, "$.id");
    }

    @Test
    void lifecycle_openToAcknowledgeToResolve_andRejectInvalidTransitions() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        String id = createAlert(farmer, createOrchard(farmer));

        // Resolve avant acknowledge -> 409
        mvc.perform(post("/api/v1/alerts/" + id + "/resolve").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Traité\"}"))
           .andExpect(status().isConflict());

        // Acknowledge -> 200
        mvc.perform(post("/api/v1/alerts/" + id + "/acknowledge").header("Authorization", manager))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
           .andExpect(jsonPath("$.acknowledgedAt").isNotEmpty());

        // Resolve sans note -> 400
        mvc.perform(post("/api/v1/alerts/" + id + "/resolve").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"  \"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.note").exists());

        // Resolve avec note -> 200
        mvc.perform(post("/api/v1/alerts/" + id + "/resolve").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"note\":\"Intervention équipe\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("RESOLVED"))
           .andExpect(jsonPath("$.resolutionNote").value("Intervention équipe"));

        // Dismiss après RESOLVED -> 409
        mvc.perform(post("/api/v1/alerts/" + id + "/dismiss").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"doublon\"}"))
           .andExpect(status().isConflict());
    }

    @Test
    void farmerSeesOnlyOwnAlerts_andOtherFarmerGets404() throws Exception {
        String a = bearer(Role.FARMER, "a@example.test");
        String b = bearer(Role.FARMER, "b@example.test");
        String idA = createAlert(a, createOrchard(a));

        mvc.perform(get("/api/v1/alerts").header("Authorization", b))
           .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/v1/alerts/" + idA).header("Authorization", b))
           .andExpect(status().isNotFound());
    }
}
