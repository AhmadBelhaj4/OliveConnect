package tn.oliveconnect.resource;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ResourceIntegrationTest extends AbstractIntegrationTest {

    @Test
    void logisticsManager_crudAllFourKinds_withExactContractKeys() throws Exception {
        String logistics = bearer(Role.LOGISTICS_MANAGER, "log@example.test");

        // 1. Worker (status omis -> AVAILABLE par défaut)
        String workerJson = mvc.perform(post("/api/v1/resources/workers").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ouvrier 01\",\"phone\":\"+216 00 000 000\",\"skills\":[\"PICKING\",\"TRACTOR\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(workerJson, "id", "fullName", "phone", "skills", "status");

        // 2. Vehicle
        String vehicleJson = mvc.perform(post("/api/v1/resources/vehicles").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"TRACTOR\",\"registration\":\"123 TUN 4567\",\"capacityKg\":3000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(vehicleJson, "id", "type", "registration", "capacityKg", "status");
        String vehicleId = JsonPath.read(vehicleJson, "$.id");

        // 3. Bin
        String binJson = mvc.perform(post("/api/v1/resources/bins").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"BIN-014\",\"capacityKg\":500}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(binJson, "id", "code", "capacityKg", "status");

        // 4. Equipment
        String eqJson = mvc.perform(post("/api/v1/resources/equipment").header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"NET\",\"label\":\"Filet de récolte\",\"quantity\":40}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(eqJson, "id", "type", "label", "quantity", "status");

        // PATCH Vehicle -> MAINTENANCE + filtre ?status=MAINTENANCE
        mvc.perform(patch("/api/v1/resources/vehicles/" + vehicleId).header("Authorization", logistics)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MAINTENANCE\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("MAINTENANCE"));

        mvc.perform(get("/api/v1/resources/vehicles?status=MAINTENANCE").header("Authorization", logistics))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void collectionManager_canRead_butCannotCreateOrPatch_403() throws Exception {
        String manager = bearer(Role.COLLECTION_MANAGER, "cm@example.test");
        mvc.perform(get("/api/v1/resources/workers").header("Authorization", manager))
           .andExpect(status().isOk());
        mvc.perform(post("/api/v1/resources/workers").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"X\",\"phone\":\"1\",\"skills\":[]}"))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void farmer_cannotReadResources_403_andUnknownKind_400() throws Exception {
        String farmer = bearer(Role.FARMER, "f@example.test");
        mvc.perform(get("/api/v1/resources/workers").header("Authorization", farmer))
           .andExpect(status().isForbidden());

        String logistics = bearer(Role.LOGISTICS_MANAGER, "log@example.test");
        mvc.perform(get("/api/v1/resources/unknown").header("Authorization", logistics))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
