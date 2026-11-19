package tn.oliveconnect.user;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tn.oliveconnect.AbstractIntegrationTest;
import tn.oliveconnect.common.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String REGISTER = """
        {"fullName":"Example Farmer","email":"Farmer@Example.test","password":"Secret#123","phone":null}""";

    @Test
    void register_returns201_withExactUserResponse_andForcedFarmerRole() throws Exception {
        String json = mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("farmer@example.test"))   // normalisé
                .andExpect(jsonPath("$.role").value("FARMER"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "id", "fullName", "email", "role", "active", "createdAt");
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
           .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Farmer@Example.test", "  farmer@example.TEST ")))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"))
           .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void password_isNeverStoredInClear() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER))
           .andExpect(status().isCreated());
        var stored = userRepository.findByEmail("farmer@example.test").orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo("Secret#123").startsWith("$2");  // BCrypt
        assertThat(passwordEncoder.matches("Secret#123", stored.getPasswordHash())).isTrue();
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTER.replace("Secret#123", "short")))
           .andExpect(status().isBadRequest())
           .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_ok_thenMe() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER));
        String json = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"farmer@example.test\",\"password\":\"Secret#123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(7200))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "accessToken", "tokenType", "expiresInSeconds", "user");
        String token = com.jayway.jsonpath.JsonPath.read(json, "$.accessToken");
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value("farmer@example.test"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"farmer@example.test\",\"password\":\"WrongPass1\"}"))
           .andExpect(status().isUnauthorized())
           .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void noToken_returns401_withContractEnvelope() throws Exception {
        String json = mvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andReturn().getResponse().getContentAsString();
        assertExactKeys(json, "timestamp", "status", "code", "message", "fieldErrors");
    }

    @Test
    void invalidToken_returns401() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer abc.def.ghi"))
           .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongRole_returns403() throws Exception {
        // POST /orchards est réservé à FARMER (§6) : un responsable de collecte reçoit 403
        String manager = bearer(Role.COLLECTION_MANAGER, "manager@example.test");
        mvc.perform(post("/api/v1/orchards").header("Authorization", manager)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                          {"name":"X","areaHectares":1,"oliveVariety":"Chemlali",
                           "location":{"type":"Point","coordinates":[10.76,34.73]},"publicVisibility":true}"""))
           .andExpect(status().isForbidden())
           .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void login_rateLimiting_returns429_onSixthFailure() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(REGISTER));

        // 5 échecs consécutifs renvoient 401
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", "192.168.1.100")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"farmer@example.test\",\"password\":\"BadPass" + i + "\"}"))
               .andExpect(status().isUnauthorized())
               .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        }

        // Le 6ème échec renvoie 429 RATE_LIMITED
        mvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", "192.168.1.100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"farmer@example.test\",\"password\":\"BadPass6\"}"))
           .andExpect(status().isTooManyRequests())
           .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
    }
}
