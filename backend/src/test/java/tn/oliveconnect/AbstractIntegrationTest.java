package tn.oliveconnect;

import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tn.oliveconnect.common.enums.Role;
import tn.oliveconnect.security.JwtService;
import tn.oliveconnect.user.document.UserDocument;
import tn.oliveconnect.user.repository.UserRepository;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mvc;
    @Autowired protected MongoTemplate mongo;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;
    @Autowired(required = false) protected org.springframework.data.redis.core.StringRedisTemplate redis;

    protected static final String ORCHARD_BODY = """
        {"name":"North Orchard","areaHectares":4.2,"oliveVariety":"Chemlali",
         "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}""";

    /** Crée un verger via l'API et renvoie son id. */
    protected String createOrchard(String farmerBearer) throws Exception {
        String json = mvc.perform(post("/api/v1/orchards").header("Authorization", farmerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(ORCHARD_BODY))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    /** Crée une demande PLANNED de 2500 kg et renvoie son id. */
    protected String createRequest(String farmerBearer, String orchardId) throws Exception {
        String body = """
            {"orchardId":"%s","type":"PLANNED","preferredDate":"%s","estimatedQuantityKg":2500,"urgencyReason":null}"""
            .formatted(orchardId, java.time.LocalDate.now().plusDays(3));
        String json = mvc.perform(post("/api/v1/harvest-requests").header("Authorization", farmerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    /** Approuve une demande (review APPROVE) en tant que responsable de collecte. */
    protected void approve(String managerBearer, String requestId) throws Exception {
        mvc.perform(post("/api/v1/harvest-requests/" + requestId + "/review").header("Authorization", managerBearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVE\",\"reason\":null}"))
           .andExpect(status().isOk());
    }

    /** Renvoie l'id (24 hex) d'un utilisateur à partir d'un "Bearer <jwt>". */
    protected String userId(String bearer) {
        return jwtService.parse(bearer.substring(7)).orElseThrow().id();
    }

    @BeforeEach
    void cleanDatabase() {
        mongo.getCollectionNames().forEach(c -> mongo.getCollection(c).deleteMany(new Document()));
        if (redis != null) {
            try {
                var keys = redis.keys("oliveconnect:*");
                if (keys != null && !keys.isEmpty()) redis.delete(keys);
            } catch (Exception ignored) {}
        }
    }

    protected static void assertExactKeys(String json, String... expected) {
        Map<String, Object> root = JsonPath.read(json, "$");
        assertThat(root.keySet()).containsExactlyInAnyOrderElementsOf(Set.of(expected));
    }

    protected String bearer(Role role, String email) {
        var u = new UserDocument();
        u.setFullName("Test " + role);
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode("Secret#123"));
        u.setRole(role);
        u.setActive(true);
        u = userRepository.save(u);
        return "Bearer " + jwtService.generate(u.getId(), u.getEmail(), u.getRole());
    }
}
