package tn.oliveconnect.notification;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void approveRequest_createsOneNotification_andMarkReadEnforcesOwner404() throws Exception {
        String farmer = bearer(Role.FARMER, "a@example.test");
        String other = bearer(Role.FARMER, "b@example.test");
        String manager = bearer(Role.COLLECTION_MANAGER, "m@example.test");

        String reqId = createRequest(farmer, createOrchard(farmer));
        approve(manager, reqId);

        String listJson = mvc.perform(get("/api/v1/notifications?unreadOnly=true").header("Authorization", farmer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].type").value("REQUEST_APPROVED"))
                .andExpect(jsonPath("$.content[0].entityType").value("HARVEST_REQUEST"))
                .andExpect(jsonPath("$.content[0].entityId").value(reqId))
                .andReturn().getResponse().getContentAsString();

        String notifId = JsonPath.read(listJson, "$.content[0].id");

        // Un autre utilisateur ne peut pas marquer la notif comme lue -> 404
        mvc.perform(post("/api/v1/notifications/" + notifId + "/read").header("Authorization", other))
           .andExpect(status().isNotFound());

        // Le destinataire la marque lue -> readAt rempli ; unreadOnly=true devient vide
        String readJson = mvc.perform(post("/api/v1/notifications/" + notifId + "/read").header("Authorization", farmer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(readJson, "id", "type", "message", "entityType", "entityId", "readAt", "createdAt");

        mvc.perform(get("/api/v1/notifications?unreadOnly=true").header("Authorization", farmer))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalElements").value(0));
    }
}
