package com.amorim.finance_manager;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryIcon;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class CategoryIntegrationTest {

    private static final String PASSWORD =
            "IntegrationPassword123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldCreateIncomeCategory() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);

        UUID categoryId = createCategory(
                token,
                "Salário",
                "INCOME",
                null
        );

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow();

        assertThat(category.getUserId()).isEqualTo(user.id());
        assertThat(category.getName()).isEqualTo("Salário");
        assertThat(category.getType()).isEqualTo(CategoryType.INCOME);
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(category.getParentCategoryId()).isNull();
    }

    @Test
    void shouldCreateExpenseCategory() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);

        UUID categoryId = createCategory(
                token,
                "Moradia",
                "EXPENSE",
                null
        );

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow();

        assertThat(category.getUserId()).isEqualTo(user.id());
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
        assertThat(category.getStatus()).isEqualTo(CategoryStatus.ACTIVE);
        assertThat(category.getIcon()).isEqualTo(CategoryIcon.TAG);
    }

    @Test
    void shouldCreateAndUpdateCategoryIcon() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);

        MvcResult createResult = mockMvc.perform(
                        post("/api/v1/categories")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "Alimentação",
                                          "icon": "TECHNOLOGY",
                                          "type": "EXPENSE",
                                          "parentCategoryId": null
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.icon").value("TECHNOLOGY"))
                .andReturn();

        UUID categoryId = UUID.fromString(
                objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText()
        );

        mockMvc.perform(
                        patch("/api/v1/categories/{id}", categoryId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"icon\":\"COMPUTER\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.icon").value("COMPUTER"));

        assertThat(categoryRepository.findById(categoryId).orElseThrow().getIcon())
                .isEqualTo(CategoryIcon.COMPUTER);
    }

    @Test
    void shouldCreateSubcategoryWithCompatibleParent()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID parentId = createCategory(
                token,
                "Moradia",
                "EXPENSE",
                null
        );

        UUID subcategoryId = createCategory(
                token,
                "Aluguel",
                "EXPENSE",
                parentId
        );

        Category subcategory = categoryRepository
                .findById(subcategoryId)
                .orElseThrow();

        assertThat(subcategory.getUserId()).isEqualTo(user.id());
        assertThat(subcategory.getParentCategoryId()).isEqualTo(parentId);
        assertThat(subcategory.getType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void shouldRejectSubcategoryWhenParentDoesNotExist()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        String body = categoryBody(
                "Aluguel",
                "EXPENSE",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/categories")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"));

        assertThat(categoryRepository.count()).isZero();
    }

    @Test
    void shouldRejectSubcategoryOwnedByAnotherUser()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        UUID parentFromUserB = createCategory(
                tokenB,
                "Moradia B",
                "EXPENSE",
                null
        );

        String body = categoryBody(
                "Aluguel A",
                "EXPENSE",
                parentFromUserB
        );

        mockMvc.perform(
                        post("/api/v1/categories")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"));

        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectSubcategoryWithIncompatibleType()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID incomeParentId = createCategory(
                token,
                "Receitas",
                "INCOME",
                null
        );

        String body = categoryBody(
                "Aluguel",
                "EXPENSE",
                incomeParentId
        );

        mockMvc.perform(
                        post("/api/v1/categories")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_TYPE_MISMATCH"));

        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldListOnlyAuthenticatedUserCategories()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        createCategory(tokenA, "Categoria A", "INCOME", null);
        createCategory(tokenB, "Categoria B", "EXPENSE", null);

        mockMvc.perform(
                        get("/api/v1/categories")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name")
                        .value("Categoria A"))
                .andExpect(jsonPath("$[0].userId")
                        .doesNotExist());
    }

    @Test
    void shouldReturnCategoryOnlyForOwner()
            throws Exception {

        TestUser userA = registerUser("User A");
        TestUser userB = registerUser("User B");

        String tokenA = login(userA);
        String tokenB = login(userB);

        UUID categoryId = createCategory(
                tokenA,
                "Salário",
                "INCOME",
                null
        );

        mockMvc.perform(
                        get("/api/v1/categories/{id}", categoryId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenA
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(categoryId.toString()))
                .andExpect(jsonPath("$.name")
                        .value("Salário"));

        mockMvc.perform(
                        get("/api/v1/categories/{id}", categoryId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tokenB
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("CATEGORY_NOT_FOUND"));
    }

    @Test
    void shouldUpdateCategoryName() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);

        UUID categoryId = createCategory(
                token,
                "Nome antigo",
                "EXPENSE",
                null
        );

        mockMvc.perform(
                        patch("/api/v1/categories/{id}", categoryId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Nome atualizado\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name")
                        .value("Nome atualizado"))
                .andExpect(jsonPath("$.type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$.status")
                        .value("ACTIVE"));

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow();

        assertThat(category.getName()).isEqualTo("Nome atualizado");
        assertThat(category.getType()).isEqualTo(CategoryType.EXPENSE);
    }

    @Test
    void shouldInactivateCategoryWithoutDeleting()
            throws Exception {

        TestUser user = registerUser("User A");
        String token = login(user);

        UUID categoryId = createCategory(
                token,
                "Moradia",
                "EXPENSE",
                null
        );

        mockMvc.perform(
                        patch("/api/v1/categories/{id}", categoryId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"INACTIVE\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("INACTIVE"));

        Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow();

        assertThat(category.getStatus())
                .isEqualTo(CategoryStatus.INACTIVE);

        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void shouldRejectEmptyPatch() throws Exception {
        TestUser user = registerUser("User A");
        String token = login(user);

        UUID categoryId = createCategory(
                token,
                "Moradia",
                "EXPENSE",
                null
        );

        mockMvc.perform(
                        patch("/api/v1/categories/{id}", categoryId)
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_CATEGORY_UPDATE"))
                .andExpect(jsonPath("$.message")
                        .value("Informe ao menos um campo para atualização"));
    }

    @Test
    void shouldRejectRequestsWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(
                        post("/api/v1/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(categoryBody(
                                        "Salário",
                                        "INCOME",
                                        null
                                ))
                )
                .andExpect(status().isUnauthorized());
    }

    private TestUser registerUser(String name) throws Exception {
        String email = "category.integration."
                + UUID.randomUUID()
                + "@example.com";

        String body = """
                {
                  "name": "%s",
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(name, email, PASSWORD);

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        return new TestUser(
                UUID.fromString(response.get("id").asText()),
                email,
                PASSWORD
        );
    }

    private String login(TestUser user) throws Exception {
        String body = """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(user.email(), user.password());

        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body)
                )
                .andExpect(status().isOk())
                .andReturn();

        return result.getResponse().getCookie("nummo_session").getValue();
    }

    private UUID createCategory(
            String token,
            String name,
            String type,
            UUID parentCategoryId
    ) throws Exception {

        MvcResult result = mockMvc.perform(
                        post("/api/v1/categories")
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(categoryBody(
                                        name,
                                        type,
                                        parentCategoryId
                                ))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value(type))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        return UUID.fromString(
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("id")
                        .asText()
        );
    }

    private String categoryBody(
            String name,
            String type,
            UUID parentCategoryId
    ) {
        String parentValue = parentCategoryId == null
                ? "null"
                : "\"" + parentCategoryId + "\"";

        return """
                {
                  "name": "%s",
                  "type": "%s",
                  "parentCategoryId": %s
                }
                """.formatted(name, type, parentValue);
    }

    private void cleanDatabase() {
        categoryRepository.findAll()
                .stream()
                .filter(category ->
                        category.getParentCategoryId() != null
                )
                .forEach(categoryRepository::delete);

        categoryRepository.flush();
        categoryRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    private record TestUser(
            UUID id,
            String email,
            String password
    ) {
    }
}
