package com.amorim.finance_manager;

import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class UserRegistrationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterUserInPostgresql() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Walter White"))
                .andExpect(jsonPath("$.email")
                        .value("walter.white@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldAuthenticateWithAnHttpOnlyCookieWithoutExposingTheJwtInJson() throws Exception {
        String registerBody = """
            {
              "name": "Jesse Pinkman",
              "email": "jesse.pinkman@example.com",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "email": "jesse.pinkman@example.com",
                              "password": "SenhaSegura123!"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("nummo_session=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        mockMvc.perform(get("/api/v1/users/me")
                        .cookie(login.getResponse().getCookie("nummo_session")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("jesse.pinkman@example.com"));

        MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf")
                        .cookie(login.getResponse().getCookie("nummo_session")))
                .andExpect(status().isOk())
                .andReturn();
        String csrfToken = objectMapper.readTree(csrf.getResponse().getContentAsString())
                .get("token")
                .asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(
                                login.getResponse().getCookie("nummo_session"),
                                new jakarta.servlet.http.Cookie("XSRF-TOKEN", csrfToken)
                        )
                        .header("X-XSRF-TOKEN", csrfToken)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void shouldReturnConflictWhenEmailAlreadyExists() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("E-mail já cadastrado"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "email-invalido",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Dados de entrada inválidos"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("email"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("E-mail inválido"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlankName() throws Exception {
        String body = """
            {
              "name": "   ",
              "email": "walter.white@example.com",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectBlankPassword()throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "   "
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectPasswordShorterThanEightCharacters() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "curta"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[?(@.message == 'Senha deve possuir entre 8 e 72 caracteres')]")
                        .isNotEmpty());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldRejectPasswordWithoutRequiredComplexity() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "SenhaSegura123"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("password"))
                .andExpect(jsonPath("$.fieldErrors[0].message")
                        .value("Senha deve conter ao menos uma letra maiúscula, uma minúscula, um número e um caractere especial"));

        assertThat(userRepository.count()).isZero();
    }

    @Test
    void shouldNormalizeEmail() throws Exception {
        String body = """
            {
              "name": "Walter White",
              "email": "  WALTER.WHITE@Example.COM  ",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("walter.white@example.com"));

        User savedUser = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedUser.getEmail())
                .isEqualTo("walter.white@example.com");
    }

    @Test
    void shouldStorePasswordAsBcrypt() throws Exception {
        String rawPassword = "SenhaSegura123!";

        String body = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "%s"
            }
            """.formatted(rawPassword);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        User savedUser = userRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow();

        assertThat(savedUser.getPasswordHash())
                .startsWith("$2");

        assertThat(passwordEncoder.matches(
                rawPassword,
                savedUser.getPasswordHash()
        )).isTrue();

        assertThat(savedUser.getPasswordHash())
                .isNotEqualTo(rawPassword);
    }

    @Test
    void shouldReturnStandardUnauthorizedPayloadForInvalidCredentials()
            throws Exception {

        String registerBody = """
            {
              "name": "Walter White",
              "email": "walter.white@example.com",
              "password": "SenhaSegura123!"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated());

        String loginBody = """
            {
              "email": "walter.white@example.com",
              "password": "senha-incorreta"
            }
            """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturnStandardBadRequestPayloadForMalformedJson()
            throws Exception {

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Requisição inválida ou malformada"))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/register"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturnStandardUnauthorizedPayloadWithoutJwt()
            throws Exception {

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value("Autenticação necessária ou token inválido"))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void shouldReturnStandardUnauthorizedPayloadForInvalidJwt()
            throws Exception {

        mockMvc.perform(get("/api/v1/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message")
                        .value("Autenticação necessária ou token inválido"))
                .andExpect(jsonPath("$.path").value("/api/v1/accounts"))
                .andExpect(jsonPath("$.fieldErrors").isArray())
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }
}
