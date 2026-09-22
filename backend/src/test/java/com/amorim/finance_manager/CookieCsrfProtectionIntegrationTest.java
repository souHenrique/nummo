package com.amorim.finance_manager;

import com.amorim.finance_manager.security.AuthCookieService;
import com.amorim.finance_manager.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class CookieCsrfProtectionIntegrationTest {

    private static final String TRUSTED_ORIGIN = "http://localhost:4200";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void rejectsCookieAuthenticatedMutationFromAnUntrustedOrigin() throws Exception {
        mockMvc.perform(post(cancelPath())
                        .cookie(authenticateCookie())
                        .header(HttpHeaders.ORIGIN, "https://malicious.example"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));
    }

    @Test
    void rejectsCookieAuthenticatedMutationWithoutOriginOrReferer() throws Exception {
        mockMvc.perform(post(cancelPath())
                        .cookie(authenticateCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
    }

    @Test
    void allowsCookieAuthenticatedMutationFromConfiguredOriginToReachTheController() throws Exception {
        mockMvc.perform(post(cancelPath())
                        .cookie(authenticateCookie())
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void acceptsAConfiguredRefererWhenOriginIsUnavailable() throws Exception {
        mockMvc.perform(post(cancelPath())
                        .cookie(authenticateCookie())
                        .header(HttpHeaders.REFERER, TRUSTED_ORIGIN + "/transactions/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    private Cookie authenticateCookie() throws Exception {
        String registration = """
                {
                  "name": "Gus Fring",
                  "email": "gus.fring@example.com",
                  "password": "SenhaSegura123!"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registration))
                .andExpect(status().isCreated());

        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "gus.fring@example.com",
                                  "password": "SenhaSegura123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie(AuthCookieService.SESSION_COOKIE_NAME);
    }

    private String cancelPath() {
        return "/api/v1/transactions/" + UUID.randomUUID() + "/cancel";
    }
}
