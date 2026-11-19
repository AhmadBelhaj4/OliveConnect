package tn.oliveconnect.map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class OrchardMapIntegrationTest extends AbstractIntegrationTest {

    @Test
    void missingBbox_returns400() throws Exception {
        mvc.perform(get("/api/v1/orchards/map"))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.bbox").exists());
    }

    @Test
    void publicVisitor_seesPublicOrchard_withPrivateFieldsNull_andLonLatOrder() throws Exception {
        String farmer = bearer(Role.FARMER, "f@example.test");
        createOrchard(farmer);   // Sfax [10.76, 34.73], publicVisibility = true

        // Visiteur anonyme : champs privés = null
        mvc.perform(get("/api/v1/orchards/map?bbox=10.5,34.5,11.0,35.0"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.type").value("FeatureCollection"))
           .andExpect(jsonPath("$.truncated").value(false))
           .andExpect(jsonPath("$.features.length()").value(1))
           .andExpect(jsonPath("$.features[0].geometry.coordinates[0]").value(10.76))
           .andExpect(jsonPath("$.features[0].geometry.coordinates[1]").value(34.73))
           .andExpect(jsonPath("$.features[0].properties.name").isEmpty())
           .andExpect(jsonPath("$.features[0].properties.farmerId").isEmpty())
           .andExpect(jsonPath("$.features[0].properties.areaHectares").isEmpty());

        // Agriculteur propriétaire : voit ses champs privés
        mvc.perform(get("/api/v1/orchards/map?bbox=10.5,34.5,11.0,35.0").header("Authorization", farmer))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.features[0].properties.name").value("North Orchard"))
           .andExpect(jsonPath("$.features[0].properties.areaHectares").value(4.2));
    }
}
