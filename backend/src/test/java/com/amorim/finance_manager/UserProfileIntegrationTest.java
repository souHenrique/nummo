package com.amorim.finance_manager;

import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.entity.UserStatus;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class UserProfileIntegrationTest {

    private static final String PASSWORD = "SenhaSegura123!";

    private static final String USER_A_EMAIL =
            "user-a@example.com";

    private static final String USER_B_EMAIL =
            "user-b@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("User A"))
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldNotReturnAnotherUsersProfile() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        registerUser(
                "User B",
                USER_B_EMAIL,
                PASSWORD
        );

        String tokenA = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL))
                .andExpect(jsonPath("$.name").value("User A"));
    }

    @Test
    void shouldUpdateCurrentUserName() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "name": "Updated User"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value(USER_A_EMAIL));

        User savedUser = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        assertThat(savedUser.getName())
                .isEqualTo("Updated User");
    }

    @Test
    void shouldUpdateCurrentUserEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "UPDATED@EXAMPLE.COM",
                  "currentPassword": "SenhaSegura123!"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email")
                        .value("updated@example.com"));

        assertThat(
                userRepository.findByEmail("updated@example.com")
        ).isPresent();

        assertThat(
                userRepository.findByEmail(USER_A_EMAIL)
        ).isEmpty();
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        registerUser(
                "User B",
                USER_B_EMAIL,
                PASSWORD
        );

        String tokenA = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "user-b@example.com",
                  "currentPassword": "SenhaSegura123!"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("EMAIL_ALREADY_EXISTS"));

        User userA = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        assertThat(userA.getEmail())
                .isEqualTo(USER_A_EMAIL);
    }

    @Test
    void shouldRejectRequestWithoutJwt() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized());

        String body = """
                {
                  "name": "Attempt Without Token"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123",
                                          "newPassword": "NovaSenhaSegura456"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        delete("/api/v1/users/me")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectEmailChangeWithoutCurrentPassword() throws Exception {
        registerUser("User A", USER_A_EMAIL, PASSWORD);
        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "updated@example.com"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        assertThat(userRepository.findByEmail(USER_A_EMAIL)).isPresent();
        assertThat(userRepository.findByEmail("updated@example.com")).isEmpty();
    }

    @Test
    void shouldRejectEmailChangeWithIncorrectCurrentPassword() throws Exception {
        registerUser("User A", USER_A_EMAIL, PASSWORD);
        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "updated@example.com",
                                          "currentPassword": "SenhaIncorreta123!"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        assertThat(userRepository.findByEmail(USER_A_EMAIL)).isPresent();
        assertThat(userRepository.findByEmail("updated@example.com")).isEmpty();
    }

    @Test
    void shouldNotModifyProtectedFields() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        User originalUser = userRepository
                .findByEmail(USER_A_EMAIL)
                .orElseThrow();

        String originalPasswordHash =
                originalUser.getPasswordHash();

        Instant originalCreatedAt =
                originalUser.getCreatedAt();

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "name": "Updated Name",
                  "id": "00000000-0000-0000-0000-000000000000",
                  "passwordHash": "fake-password",
                  "createdAt": "2000-01-01T00:00:00Z",
                  "updatedAt": "2000-01-01T00:00:00Z"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(originalUser.getId().toString()))
                .andExpect(jsonPath("$.passwordHash")
                        .doesNotExist());

        User updatedUser = userRepository
                .findById(originalUser.getId())
                .orElseThrow();

        assertThat(updatedUser.getId())
                .isEqualTo(originalUser.getId());

        assertThat(updatedUser.getPasswordHash())
                .isEqualTo(originalPasswordHash);

        assertThat(updatedUser.getCreatedAt())
                .isEqualTo(originalCreatedAt);

        assertThat(updatedUser.getName())
                .isEqualTo("Updated Name");
    }

    @Test
    void shouldRejectEmptyPatch() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectInvalidEmail() throws Exception {
        registerUser(
                "User A",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(
                USER_A_EMAIL,
                PASSWORD
        );

        String body = """
                {
                  "email": "invalid-email"
                }
                """;

        mockMvc.perform(
                        patch("/api/v1/users/me")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldChangePasswordWhenCurrentPasswordIsValid() throws Exception {
        registerUser(
                "Walter White",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(USER_A_EMAIL, PASSWORD);
        String newPassword = "NovaSenhaSegura456!";

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123!",
                                          "newPassword": "NovaSenhaSegura456!"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();

        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches(PASSWORD, updatedUser.getPasswordHash())).isFalse();

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "user-a@example.com",
                                          "password": "SenhaSegura123"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());

        String newToken = login(USER_A_EMAIL, newPassword);

        assertThat(newToken).isNotBlank();
    }

    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsInvalid() throws Exception {
        registerUser(
                "Skyler White",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaIncorreta999",
                                          "newPassword": "NovaSenhaSegura456!"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_CHANGE"));

        User unchangedUser = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();

        assertThat(passwordEncoder.matches(PASSWORD, unchangedUser.getPasswordHash())).isTrue();
    }

    @Test
    void shouldRejectInvalidOrUnchangedNewPassword() throws Exception {
        registerUser(
                "Gustavo Fring",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123!",
                                          "newPassword": "curta"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123!",
                                          "newPassword": "NovaSenhaSegura456"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("newPassword"));

        mockMvc.perform(
                        patch("/api/v1/users/me/password")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123!",
                                          "newPassword": "SenhaSegura123!"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_CHANGE"));
    }

    @Test
    void shouldSoftDeleteCurrentUserAndInvalidateExistingSessions() throws Exception {
        registerUser(
                "Saul Goodman",
                USER_A_EMAIL,
                PASSWORD
        );

        String token = login(USER_A_EMAIL, PASSWORD);
        User userBeforeDeletion = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();

        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaSegura123!"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());

        User deletedUser = userRepository.findByEmail(USER_A_EMAIL).orElseThrow();

        assertThat(deletedUser.getId()).isEqualTo(userBeforeDeletion.getId());
        assertThat(deletedUser.getStatus()).isEqualTo(UserStatus.DELETED);
        assertThat(deletedUser.getAuthenticationVersion())
                .isEqualTo(userBeforeDeletion.getAuthenticationVersion() + 1);

        mockMvc.perform(
                        get("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                )
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "email": "user-a@example.com",
                                          "password": "SenhaSegura123!"
                                        }
                                        """)
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectAccountDeletionWithoutCurrentPassword() throws Exception {
        registerUser("Saul Goodman", USER_A_EMAIL, PASSWORD);
        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(userRepository.findByEmail(USER_A_EMAIL).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void shouldRejectAccountDeletionWithIncorrectCurrentPassword() throws Exception {
        registerUser("Saul Goodman", USER_A_EMAIL, PASSWORD);
        String token = login(USER_A_EMAIL, PASSWORD);

        mockMvc.perform(
                        delete("/api/v1/users/me")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "currentPassword": "SenhaIncorreta123!"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CURRENT_PASSWORD"));

        assertThat(userRepository.findByEmail(USER_A_EMAIL).orElseThrow().getStatus())
                .isEqualTo(UserStatus.ACTIVE);
    }

    private void registerUser(
            String name,
            String email,
            String password
    ) throws Exception {

        String body = """
                {
                  "name": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(name, email, password);

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated());
    }

    private String login(
            String email,
            String password
    ) throws Exception {

        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("nummo_session").getValue();
    }
}
