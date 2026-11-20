package tn.oliveconnect.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

class ImportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void scenarioE_partialImport_thenReimportProducesZeroDuplicates() throws Exception {
        bearer(Role.FARMER, "farmer@example.test");
        String admin = bearer(Role.ADMIN, "admin@example.test");

        String csv = """
                externalId,farmerEmail,name,areaHectares,oliveVariety,longitude,latitude,publicVisibility
                EXT-O1,farmer@example.test,Verger Import 1,3.5,Chemlali,10.76,34.73,true
                EXT-O2,farmer@example.test,Verger Latitude Ko,2.0,Chemlali,10.76,120.0,true
                """;
        var file = new MockMultipartFile("file", "orchards.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        // 1er import : 1 inséré, 1 rejeté (latitude hors [-90, 90]) -> PARTIAL
        String json1 = mvc.perform(multipart("/api/v1/admin/imports").file(file)
                        .param("type", "ORCHARDS").header("Authorization", admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARTIAL"))
                .andExpect(jsonPath("$.insertedCount").value(1))
                .andExpect(jsonPath("$.rejectedCount").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("latitude"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json1, "id", "type", "sourceFile", "status", "startedAt", "finishedAt",
                "readCount", "insertedCount", "updatedCount", "rejectedCount", "errors");

        // 2e import du même fichier : 0 inséré, 1 mis à jour -> 0 doublon !
        mvc.perform(multipart("/api/v1/admin/imports").file(file)
                        .param("type", "ORCHARDS").header("Authorization", admin))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.insertedCount").value(0))
           .andExpect(jsonPath("$.updatedCount").value(1));

        assertThat(mongo.getCollection("orchards").countDocuments()).isEqualTo(1);
    }
}
