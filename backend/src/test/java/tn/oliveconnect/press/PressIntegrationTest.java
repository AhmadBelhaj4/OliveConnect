package tn.oliveconnect.press;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class PressIntegrationTest extends AbstractIntegrationTest {

    @Test
    void reception_201_thenDuplicate409_andOverCapacity400() throws Exception {
        String pressManager = bearer(Role.PRESS_MANAGER, "pm@example.test");

        Document press = new Document("name", "Pressoir Nord")
                .append("location", new Document("type", "Point").append("coordinates", List.of(10.75, 34.74)))
                .append("dailyCapacityKg", 50000).append("availableCapacityKg", 32000);
        mongo.getCollection("presses").insertOne(press);
        String pressId = press.getObjectId("_id").toHexString();

        Document bin = new Document("code", "BIN-014").append("capacityKg", 500).append("status", "AVAILABLE");
        mongo.getCollection("bins").insertOne(bin);
        String binId = bin.getObjectId("_id").toHexString();

        Document tour = new Document("tourDate", LocalDate.of(2026, 11, 20)).append("status", "IN_PROGRESS");
        mongo.getCollection("tours").insertOne(tour);
        String tourId = tour.getObjectId("_id").toHexString();

        // Poids négatif -> 400
        String badNeg = """
            {"tourId":"%s","binId":"%s","pressId":"%s","weightKg":-10,"qualityGrade":"A","notes":null,"receivedAt":null}"""
            .formatted(tourId, binId, pressId);
        mvc.perform(post("/api/v1/press-receptions").header("Authorization", pressManager)
                        .contentType(MediaType.APPLICATION_JSON).content(badNeg))
           .andExpect(status().isBadRequest());

        // Poids > capacité benne (500 kg) -> 400
        String overCap = badNeg.replace("-10", "600");
        mvc.perform(post("/api/v1/press-receptions").header("Authorization", pressManager)
                        .contentType(MediaType.APPLICATION_JSON).content(overCap))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.weightKg").exists());

        // Nominal -> 201 avec exactement 9 clés
        String ok = badNeg.replace("-10", "450");
        String json = mvc.perform(post("/api/v1/press-receptions").header("Authorization", pressManager)
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qualityGrade").value("A"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "tourId", "binId", "pressId", "receivedAt", "weightKg",
                "qualityGrade", "notes", "recordedBy");

        // Doublon (tourId, binId) -> 409 DUPLICATE_RESOURCE
        mvc.perform(post("/api/v1/press-receptions").header("Authorization", pressManager)
                        .contentType(MediaType.APPLICATION_JSON).content(ok))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
    }
}
