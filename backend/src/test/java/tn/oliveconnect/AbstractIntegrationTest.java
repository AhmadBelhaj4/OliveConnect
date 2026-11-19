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

    @BeforeEach
    void cleanDatabase() {
        mongo.getCollectionNames().forEach(c -> mongo.getCollection(c).deleteMany(new Document()));
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
