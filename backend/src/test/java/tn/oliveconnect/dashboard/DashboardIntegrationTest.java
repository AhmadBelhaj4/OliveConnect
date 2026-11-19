package tn.oliveconnect.dashboard;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class DashboardIntegrationTest extends AbstractIntegrationTest {

    private String managerToken;
    private String farmerToken;

    @BeforeEach
    void setUpFixture() {
        managerToken = bearer(Role.COLLECTION_MANAGER, "manager@example.test");
        farmerToken = bearer(Role.FARMER, "farmer@example.test");

        Instant tRequested1 = Instant.parse("2026-11-01T08:00:00Z");
        Instant tScheduled1 = Instant.parse("2026-11-02T08:00:00Z"); // +24h
        Instant tCompleted1 = Instant.parse("2026-11-03T08:00:00Z"); // +24h

        Instant tRequested2 = Instant.parse("2026-11-05T10:00:00Z");
        Instant tScheduled2 = Instant.parse("2026-11-05T22:00:00Z"); // +12h
        Instant tCompleted2 = Instant.parse("2026-11-06T10:00:00Z"); // +12h

        ObjectId o1 = new ObjectId();
        ObjectId o2 = new ObjectId();

        // 1 & 2. Two COMPLETED harvest requests
        mongo.getCollection("harvest_requests").insertOne(new Document("_id", new ObjectId())
                .append("orchardId", o1)
                .append("orchardName", "Verger Sfax")
                .append("status", "COMPLETED")
                .append("actualQuantityKg", 1500.0)
                .append("requestedAt", tRequested1)
                .append("scheduledAt", tScheduled1)
                .append("completedAt", tCompleted1));

        mongo.getCollection("harvest_requests").insertOne(new Document("_id", new ObjectId())
                .append("orchardId", o2)
                .append("orchardName", "Verger Thyna")
                .append("status", "COMPLETED")
                .append("actualQuantityKg", 2500.0)
                .append("requestedAt", tRequested2)
                .append("scheduledAt", tScheduled2)
                .append("completedAt", tCompleted2));

        // 3. One SUBMITTED harvest request
        mongo.getCollection("harvest_requests").insertOne(new Document("_id", new ObjectId())
                .append("orchardId", o1)
                .append("orchardName", "Verger Sfax")
                .append("status", "SUBMITTED")
                .append("requestedAt", Instant.parse("2026-11-10T10:00:00Z")));

        // 4 & 5. Two COMPLETED tours
        ObjectId tour1 = new ObjectId();
        ObjectId tour2 = new ObjectId();
        mongo.getCollection("tours").insertOne(new Document("_id", tour1)
                .append("status", "COMPLETED")
                .append("completedAt", Instant.parse("2026-11-03T18:00:00Z")));

        mongo.getCollection("tours").insertOne(new Document("_id", tour2)
                .append("status", "COMPLETED")
                .append("completedAt", Instant.parse("2026-11-06T18:00:00Z")));

        // 6. One OPEN field alert
        mongo.getCollection("field_alerts").insertOne(new Document("_id", new ObjectId())
                .append("status", "OPEN")
                .append("priority", "HIGH")
                .append("createdAt", Instant.parse("2026-11-05T12:00:00Z")));

        // 7 & 8. Two bins
        ObjectId bin1 = new ObjectId();
        ObjectId bin2 = new ObjectId();
        mongo.getCollection("bins").insertOne(new Document("_id", bin1)
                .append("code", "BIN-01")
                .append("capacityKg", 500));
        mongo.getCollection("bins").insertOne(new Document("_id", bin2)
                .append("code", "BIN-02")
                .append("capacityKg", 600));

        // 9 & 10. Two press receptions for bin1 (400 kg & 450 kg -> 850 kg total; rates 0.8 & 0.9 -> avg 0.85)
        mongo.getCollection("press_receptions").insertOne(new Document("_id", new ObjectId())
                .append("tourId", tour1.toHexString())
                .append("binId", bin1.toHexString())
                .append("weightKg", 400.0)
                .append("receivedAt", Instant.parse("2026-11-03T19:00:00Z")));

        mongo.getCollection("press_receptions").insertOne(new Document("_id", new ObjectId())
                .append("tourId", tour2.toHexString())
                .append("binId", bin1.toHexString())
                .append("weightKg", 450.0)
                .append("receivedAt", Instant.parse("2026-11-06T19:00:00Z")));
    }

    @Test
    void summary_exactValuesAndContractKeys() throws Exception {
        String json = mvc.perform(get("/api/v1/dashboard/summary?from=2026-11-01&to=2026-11-30")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHarvestedKg").value(4000.0))
                .andExpect(jsonPath("$.totalReceivedKg").value(850.0))
                .andExpect(jsonPath("$.completedTours").value(2))
                .andExpect(jsonPath("$.openAlerts").value(1))
                .andExpect(jsonPath("$.requestsByStatus.COMPLETED").value(2))
                .andExpect(jsonPath("$.requestsByStatus.SUBMITTED").value(1))
                .andReturn().getResponse().getContentAsString();

        assertExactKeys(json, "from", "to", "totalHarvestedKg", "totalReceivedKg",
                "completedTours", "openAlerts", "requestsByStatus");
    }

    @Test
    void yieldByOrchard_exactValues() throws Exception {
        mvc.perform(get("/api/v1/dashboard/yield-by-orchard?from=2026-11-01&to=2026-11-30")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].orchardName").value("Verger Thyna"))
                .andExpect(jsonPath("$.items[0].totalHarvestedKg").value(2500.0))
                .andExpect(jsonPath("$.items[1].orchardName").value("Verger Sfax"))
                .andExpect(jsonPath("$.items[1].totalHarvestedKg").value(1500.0));
    }

    @Test
    void collectionDelays_exactValues() throws Exception {
        mvc.perform(get("/api/v1/dashboard/collection-delays?from=2026-11-01&to=2026-11-30")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sampleSize").value(2))
                .andExpect(jsonPath("$.avgSubmittedToScheduledHours").value(18.0)) // (24+12)/2 = 18.0
                .andExpect(jsonPath("$.avgScheduledToCompletedHours").value(18.0)); // (24+12)/2 = 18.0
    }

    @Test
    void binFillRates_exactValues() throws Exception {
        mvc.perform(get("/api/v1/dashboard/bin-fill-rates?from=2026-11-01&to=2026-11-30")
                        .header("Authorization", managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].binCode").value("BIN-01"))
                .andExpect(jsonPath("$.items[0].capacityKg").value(500))
                .andExpect(jsonPath("$.items[0].receptionCount").value(2))
                .andExpect(jsonPath("$.items[0].avgFillRate").value(0.85));
    }

    @Test
    void securityAndValidation_rangeAndRoles() throws Exception {
        // Période > 366 jours -> 400
        mvc.perform(get("/api/v1/dashboard/summary?from=2024-01-01&to=2026-01-01")
                        .header("Authorization", managerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        // Rôle FARMER -> 403
        mvc.perform(get("/api/v1/dashboard/summary?from=2026-11-01&to=2026-11-30")
                        .header("Authorization", farmerToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
