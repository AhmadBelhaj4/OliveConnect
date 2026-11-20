package tn.oliveconnect.tour;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class TourIntegrationTest extends AbstractIntegrationTest {

    String createResource(String logistics, String kind, String json) throws Exception {
        String res = mvc.perform(post("/api/v1/resources/" + kind).header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(res, "$.id");
    }

    @Test
    void scenarioD_sameVehicleSameDay_returns409ResourceConflict_andUnassignedLeaderGets403() throws Exception {
        String farmer = bearer(Role.FARMER, "f@example.test");
        String manager = bearer(Role.COLLECTION_MANAGER, "cm@example.test");
        String logistics = bearer(Role.LOGISTICS_MANAGER, "lm@example.test");
        String leader1 = bearer(Role.TEAM_LEADER, "tl1@example.test");
        String leader2 = bearer(Role.TEAM_LEADER, "tl2@example.test");

        String orchardId = createOrchard(farmer);
        String req1 = createRequest(farmer, orchardId);
        String req2 = createRequest(farmer, orchardId);
        approve(manager, req1);
        approve(manager, req2);

        String w1 = createResource(logistics, "workers", "{\"fullName\":\"O1\",\"phone\":\"1\",\"skills\":[\"PICKING\"]}");
        String w2 = createResource(logistics, "workers", "{\"fullName\":\"O2\",\"phone\":\"2\",\"skills\":[\"PICKING\"]}");
        String v1 = createResource(logistics, "vehicles", "{\"type\":\"TRACTOR\",\"registration\":\"100 TUN 1\",\"capacityKg\":3000}");
        String b1 = createResource(logistics, "bins", "{\"code\":\"BIN-1\",\"capacityKg\":500}");
        String b2 = createResource(logistics, "bins", "{\"code\":\"BIN-2\",\"capacityKg\":500}");

        // Tournée 1 : DRAFT -> PLANNED OK
        String t1Json = mvc.perform(post("/api/v1/tours").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tourDate\":\"2026-11-20\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String t1 = JsonPath.read(t1Json, "$.id");

        String plan1 = """
            {"teamLeaderId":"%s","vehicleIds":["%s"],"workerIds":["%s"],"binIds":["%s"],
             "stops":[{"harvestRequestId":"%s","sequence":1}]}"""
            .formatted(userId(leader1), v1, w1, b1, req1);
        String planned = mvc.perform(post("/api/v1/tours/" + t1 + "/plan").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content(plan1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(planned, "id", "tourDate", "status", "teamLeaderId", "vehicleIds", "workerIds",
                "binIds", "stops", "startedAt", "completedAt", "createdAt", "updatedAt");

        // Tournée 2 le MÊME JOUR avec le MÊME véhicule v1 -> 409 RESOURCE_CONFLICT (Scénario D)
        String t2Json = mvc.perform(post("/api/v1/tours").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tourDate\":\"2026-11-20\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String t2 = JsonPath.read(t2Json, "$.id");
        String plan2 = """
            {"teamLeaderId":"%s","vehicleIds":["%s"],"workerIds":["%s"],"binIds":["%s"],
             "stops":[{"harvestRequestId":"%s","sequence":1}]}"""
            .formatted(userId(leader2), v1, w2, b2, req2);
        mvc.perform(post("/api/v1/tours/" + t2 + "/plan").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content(plan2))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("RESOURCE_CONFLICT"))
           .andExpect(jsonPath("$.fieldErrors.vehicleIds").exists());

        // Chef d'équipe non assigné tente de démarrer t1 -> 403 ACCESS_DENIED
        mvc.perform(post("/api/v1/tours/" + t1 + "/start").header("Authorization", leader2))
           .andExpect(status().isForbidden());

        // Chef assigné démarre, complète l'arrêt puis clôture -> demande passe à COMPLETED
        String stopId = JsonPath.read(planned, "$.stops[0].stopId");
        mvc.perform(post("/api/v1/tours/" + t1 + "/start").header("Authorization", leader1))
           .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(post("/api/v1/tours/" + t1 + "/complete").header("Authorization", leader1))
           .andExpect(status().isConflict());   // arrêt non terminé -> 409
        mvc.perform(post("/api/v1/tours/" + t1 + "/stops/" + stopId + "/complete").header("Authorization", leader1)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"actualQuantityKg\":2380,\"note\":null}"))
           .andExpect(status().isOk()).andExpect(jsonPath("$.stops[0].status").value("COMPLETED"));
        mvc.perform(post("/api/v1/tours/" + t1 + "/complete").header("Authorization", leader1))
           .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        // Retour agriculteur : la demande est COMPLETED avec actualQuantityKg = 2380
        mvc.perform(get("/api/v1/harvest-requests/" + req1).header("Authorization", farmer))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("COMPLETED"))
           .andExpect(jsonPath("$.actualQuantityKg").value(2380));
    }
}
