package tn.oliveconnect.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

/**
 * Exécute la matrice §4 (Visiteur + 6 rôles × endpoints S1+S2) et génère target/security-matrix.md.
 * Chaque case interdite doit renvoyer 401 (visiteur) ou 403 (mauvais rôle).
 */
class SecurityMatrixTest extends AbstractIntegrationTest {

    record Case(String label, String method, String uri, String body, Map<String, Integer> expected) {}

    @Test
    void verifyRoleMatrix_andExportMarkdownTable() throws Exception {
        Map<String, String> tokens = new LinkedHashMap<>();
        tokens.put("VISITOR", null);
        for (Role r : Role.values()) {
            tokens.put(r.name(), bearer(r, r.name().toLowerCase() + "@matrix.test"));
        }

        // Exemples clés de la matrice §4
        List<Case> cases = List.of(
            new Case("GET /users/me", "GET", "/api/v1/users/me", null,
                Map.of("VISITOR", 401, "FARMER", 200, "COLLECTION_MANAGER", 200, "TEAM_LEADER", 200,
                       "LOGISTICS_MANAGER", 200, "PRESS_MANAGER", 200, "ADMIN", 200)),
            new Case("GET /orchards/mine", "GET", "/api/v1/orchards/mine", null,
                Map.of("VISITOR", 401, "FARMER", 200, "COLLECTION_MANAGER", 403, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 403, "PRESS_MANAGER", 403, "ADMIN", 403)),
            new Case("GET /harvest-requests", "GET", "/api/v1/harvest-requests", null,
                Map.of("VISITOR", 401, "FARMER", 403, "COLLECTION_MANAGER", 200, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 403, "PRESS_MANAGER", 403, "ADMIN", 200)),
            new Case("GET /resources/workers", "GET", "/api/v1/resources/workers", null,
                Map.of("VISITOR", 401, "FARMER", 403, "COLLECTION_MANAGER", 200, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 200, "PRESS_MANAGER", 403, "ADMIN", 200)),
            new Case("POST /tours", "POST", "/api/v1/tours", "{\"tourDate\":\"2026-11-20\"}",
                Map.of("VISITOR", 401, "FARMER", 403, "COLLECTION_MANAGER", 201, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 403, "PRESS_MANAGER", 403, "ADMIN", 403)),
            new Case("GET /orchards/map?bbox=...", "GET", "/api/v1/orchards/map?bbox=10.5,34.5,11.0,35.0", null,
                Map.of("VISITOR", 200, "FARMER", 200, "COLLECTION_MANAGER", 200, "TEAM_LEADER", 200,
                       "LOGISTICS_MANAGER", 200, "PRESS_MANAGER", 200, "ADMIN", 200)),
            new Case("GET /dashboard/summary", "GET", "/api/v1/dashboard/summary?from=2026-11-01&to=2026-11-30", null,
                Map.of("VISITOR", 401, "FARMER", 403, "COLLECTION_MANAGER", 200, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 403, "PRESS_MANAGER", 200, "ADMIN", 200)),
            new Case("GET /admin/users", "GET", "/api/v1/admin/users", null,
                Map.of("VISITOR", 401, "FARMER", 403, "COLLECTION_MANAGER", 403, "TEAM_LEADER", 403,
                       "LOGISTICS_MANAGER", 403, "PRESS_MANAGER", 403, "ADMIN", 200))
        );

        StringBuilder md = new StringBuilder("| Endpoint | " + String.join(" | ", tokens.keySet()) + " |\n");
        md.append("|---|" + "---|".repeat(tokens.size()) + "\n");

        for (Case c : cases) {
            md.append("| `").append(c.label()).append("` |");
            for (var entry : tokens.entrySet()) {
                var req = "POST".equals(c.method())
                        ? post(c.uri()).contentType(MediaType.APPLICATION_JSON).content(c.body())
                        : get(c.uri());
                if (entry.getValue() != null) req.header("Authorization", entry.getValue());
                int expectedStatus = c.expected().get(entry.getKey());
                mvc.perform(req).andExpect(status().is(expectedStatus));
                md.append(" ").append(expectedStatus).append(" |");
            }
            md.append("\n");
        }
        new File("target").mkdirs();
        Files.writeString(Path.of("target/security-matrix.md"), md.toString());
    }
}
