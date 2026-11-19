package tn.oliveconnect.harvest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class HarvestTransitionIntegrationTest extends AbstractIntegrationTest {

    String farmer, otherFarmer, manager, requestId;

    @BeforeEach
    void setUp() throws Exception {
        farmer = bearer(Role.FARMER, "a@example.test");
        otherFarmer = bearer(Role.FARMER, "b@example.test");
        manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");
        requestId = createRequest(farmer, createOrchard(farmer));
    }

    @Test
    void approve_200_thenSecondReview_409() throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"reason\":null}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("APPROVED"))
           .andExpect(jsonPath("$.reviewedBy").value(userId(manager)));
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECT\",\"reason\":\"x\"}"))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("INVALID_STATE_TRANSITION"));
    }

    @Test
    void reject_withoutReason_400() throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECT\",\"reason\":\"  \"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.reason").exists());
    }

    @Test
    void farmerCannotReview_403() throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"reason\":null}"))
           .andExpect(status().isForbidden());
    }

    @Test
    void farmerCancelsOwn_200_thenCancelAgain_409() throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/cancel").header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Pluie annoncée\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/cancel").header("Authorization", farmer))
           .andExpect(status().isConflict());
    }

    @Test
    void otherFarmerCancel_404() throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/cancel").header("Authorization", otherFarmer))
           .andExpect(status().isNotFound());
    }

    @Test
    void cancelInProgress_409() throws Exception {
        mongo.getCollection("harvest_requests").updateOne(new Document("_id", new ObjectId(requestId)),
                new Document("$set", new Document("status", "IN_PROGRESS")));
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/cancel").header("Authorization", manager))
           .andExpect(status().isConflict());
    }

    @Test
    void cancelScheduled_callsRemoveStop_andCancels() throws Exception {
        // Pas de tournée réelle : TourFacadeImpl ne trouve rien et ne fait rien -> l'annulation passe.
        mongo.getCollection("harvest_requests").updateOne(new Document("_id", new ObjectId(requestId)),
                new Document("$set", new Document("status", "SCHEDULED")));
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/cancel").header("Authorization", farmer))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("CANCELLED"))
           .andExpect(jsonPath("$.scheduledTourId").isEmpty());
    }

    @Test
    void patch_inSubmitted_200_afterReject_409() throws Exception {
        mvc.perform(patch("/api/v1/harvest-requests/" + requestId).header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estimatedQuantityKg\":3000}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.estimatedQuantityKg").value(3000));
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECT\",\"reason\":\"Trop tôt\"}"))
           .andExpect(status().isOk());
        mvc.perform(patch("/api/v1/harvest-requests/" + requestId).header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"estimatedQuantityKg\":1000}"))
           .andExpect(status().isConflict());
    }

    @Test
    void patch_toUrgentWithoutReason_400() throws Exception {
        mvc.perform(patch("/api/v1/harvest-requests/" + requestId).header("Authorization", farmer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"URGENT\"}"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.urgencyReason").exists());
    }
}
