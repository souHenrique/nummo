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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class CsrfProtectionIntegrationTest {

    private static final String TRUSTED_ORIGIN = "http://localhost:4200";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void issuesANonCacheableCsrfTokenAndAnHttpOnlyCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString(CSRF_COOKIE_NAME + "=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.headerName").value(CSRF_HEADER_NAME));
    }

    @Test
    void rejectsCookieAuthenticatedMutationWithoutCsrfToken() throws Exception {
        mockMvc.perform(post(cancelPath())
                        .cookie(authenticateCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
    }

    @Test
    void rejectsCookieAuthenticatedMutationWithInvalidCsrfToken() throws Exception {
        Cookie sessionCookie = authenticateCookie();
        CsrfContext csrf = csrfContext(sessionCookie);

        mockMvc.perform(post(cancelPath())
                        .cookie(sessionCookie, csrf.cookie())
                        .header(CSRF_HEADER_NAME, "invalid-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_VALIDATION_FAILED"));
    }

    @Test
    void allowsCookieAuthenticatedMutationWithMatchingCookieAndHeader() throws Exception {
        Cookie sessionCookie = authenticateCookie();
        CsrfContext csrf = csrfContext(sessionCookie);

        mockMvc.perform(post(cancelPath())
                        .cookie(sessionCookie, csrf.cookie())
                        .header(CSRF_HEADER_NAME, csrf.token()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
    }

    @Test
    void stillRejectsAnUntrustedCrossOriginRequestWithAValidToken() throws Exception {
        Cookie sessionCookie = authenticateCookie();
        CsrfContext csrf = csrfContext(sessionCookie);

        mockMvc.perform(post(cancelPath())
                        .cookie(sessionCookie, csrf.cookie())
                        .header(CSRF_HEADER_NAME, csrf.token())
                        .header(HttpHeaders.ORIGIN, "https://malicious.example"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Invalid CORS request"));
    }

    private CsrfContext csrfContext(Cookie sessionCookie) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf")
                        .cookie(sessionCookie)
                        .header(HttpHeaders.ORIGIN, TRUSTED_ORIGIN))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = response.get("token").asText();
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String cookieValue = setCookie.substring(
                (CSRF_COOKIE_NAME + "=").length(),
                setCookie.indexOf(';')
        );

        assertThat(cookieValue).isEqualTo(token);

        return new CsrfContext(token, new Cookie(CSRF_COOKIE_NAME, cookieValue));
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

    private record CsrfContext(String token, Cookie cookie) {
    }
}
