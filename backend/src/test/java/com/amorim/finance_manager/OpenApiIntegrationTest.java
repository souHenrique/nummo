package com.amorim.finance_manager;

import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class OpenApiIntegrationTest {

    private static final List<String> OPERATIONS = List.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login",
            "post /api/v1/auth/logout",
            "get /api/v1/auth/csrf",
            "get /api/v1/users/me",
            "patch /api/v1/users/me",
            "patch /api/v1/users/me/password",
            "delete /api/v1/users/me",
            "post /api/v1/accounts",
            "get /api/v1/accounts",
            "get /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}/status",
            "post /api/v1/credit-cards",
            "get /api/v1/credit-cards",
            "get /api/v1/credit-cards/{id}",
            "patch /api/v1/credit-cards/{id}",
            "post /api/v1/credit-cards/{id}/purchases",
            "get /api/v1/invoices",
            "get /api/v1/invoices/{id}",
            "get /api/v1/credit-cards/{id}/invoices",
            "post /api/v1/categories",
            "get /api/v1/categories",
            "get /api/v1/categories/{id}",
            "patch /api/v1/categories/{id}",
            "post /api/v1/transactions",
            "get /api/v1/transactions",
            "get /api/v1/transactions/{id}",
            "get /api/v1/transactions/{id}/installments",
            "patch /api/v1/transactions/{id}",
            "post /api/v1/transactions/{id}/cancel",
            "post /api/v1/transfers",
            "get /api/v1/reports/cash/daily",
            "get /api/v1/reports/cash/weekly",
            "get /api/v1/reports/cash/monthly",
            "get /api/v1/reports/cash/annual",
            "get /api/v1/reports/competence",
            "post /api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund",
            "post /api/v1/invoices/{id}/close",
            "post /api/v1/invoices/{id}/pay",
            "post /api/v1/budgets",
            "get /api/v1/budgets",
            "get /api/v1/budgets/{id}",
            "patch /api/v1/budgets/{id}",
            "delete /api/v1/budgets/{id}",
            "get /api/v1/dashboard",
            "get /api/v1/exports/transactions.csv"
    );

    private static final Set<String> OPERATIONS_WITH_REQUEST_BODY = Set.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login",
            "patch /api/v1/users/me",
            "patch /api/v1/users/me/password",
            "post /api/v1/accounts",
            "patch /api/v1/accounts/{id}",
            "patch /api/v1/accounts/{id}/status",
            "post /api/v1/credit-cards",
            "patch /api/v1/credit-cards/{id}",
            "post /api/v1/credit-cards/{id}/purchases",
            "post /api/v1/categories",
            "patch /api/v1/categories/{id}",
            "post /api/v1/transactions",
            "patch /api/v1/transactions/{id}",
            "post /api/v1/transfers",
            "post /api/v1/credit-cards/{creditCardId}/purchase/{transactionId}/refund",
            "post /api/v1/invoices/{id}/close",
            "post /api/v1/invoices/{id}/pay",
            "post /api/v1/budgets",
            "patch /api/v1/budgets/{id}"
    );

    private static final Set<String> PUBLIC_OPERATIONS = Set.of(
            "post /api/v1/auth/register",
            "post /api/v1/auth/login",
            "post /api/v1/auth/logout",
            "get /api/v1/auth/csrf"
    );

    private static final List<String> PUBLIC_SCHEMAS = List.of(
            "RegisterRequest",
            "LoginRequest",
            "AuthResponse",
            "UpdateProfileRequest",
            "ChangePasswordRequest",
            "UserResponse",
            "CreateAccountRequest",
            "UpdateAccountRequest",
            "UpdateAccountStatusRequest",
            "AccountResponse",
            "CreateCreditCardRequest",
            "UpdateCreditCardRequest",
            "CreateCreditCardPurchaseRequest",
            "CreditCardResponse",
            "InvoiceSummaryResponse",
            "InvoiceDetailResponse",
            "InvoicePageResponse",
            "CreateCategoryRequest",
            "UpdateCategoryRequest",
            "CategoryResponse",
            "CreateTransactionRequest",
            "UpdateTransactionRequest",
            "TransactionResponse",
            "TransactionListItemResponse",
            "TransactionInstallmentDetailsResponse",
            "TransactionPageResponse",
            "CategoryCashFlowResponse",
            "CashFlowSummaryResponse",
            "DailyCashFlowResponse",
            "CashFlowPeriodResponse",
            "CashFlowComparisonResponse",
            "WeeklyCashFlowResponse",
            "CompetenceReportResponse",
            "CreateTransferRequest",
            "ApiError",
            "FieldErrorResponse",
            "CreateBudgetRequest",
            "UpdateBudgetRequest",
            "BudgetResponse",
            "DashboardIndicatorResponse",
            "DashboardBudgetItemResponse",
            "DashboardBudgetResponse",
            "DashboardResponse"
    );

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldExposeSwaggerUiAndOpenApiWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldPublishCompleteAndSecuredEndpointDocumentation() throws Exception {
        JsonNode document = loadOpenApiDocument();

        assertThat(document.path("openapi").asString()).startsWith("3.1");
        assertThat(document.path("info").path("title").asString())
                .isEqualTo("Nummo API");

        JsonNode bearerAuth = document.path("components")
                .path("securitySchemes")
                .path("bearerAuth");

        assertThat(bearerAuth.path("type").asString()).isEqualTo("http");
        assertThat(bearerAuth.path("scheme").asString()).isEqualTo("bearer");
        assertThat(bearerAuth.path("bearerFormat").asString()).isEqualTo("JWT");
        assertThat(countOperations(document.path("paths"))).isEqualTo(OPERATIONS.size());

        for (String operationKey : OPERATIONS) {
            JsonNode operation = findOperation(document, operationKey);

            assertThat(operation.isMissingNode())
                    .as("operação %s deve existir", operationKey)
                    .isFalse();
            assertThat(operation.path("summary").asString())
                    .as("summary de %s", operationKey)
                    .isNotBlank();
            assertThat(operation.path("description").asString())
                    .as("description de %s", operationKey)
                    .isNotBlank();

            assertSuccessResponseHasExample(operation, operationKey);
            assertErrorResponsesUseApiError(operation, operationKey);

            if (OPERATIONS_WITH_REQUEST_BODY.contains(operationKey)) {
                JsonNode requestBody = operation.path("requestBody");
                assertThat(requestBody.isMissingNode()).as(operationKey).isFalse();
                assertThat(requestBody.path("required").asBoolean()).as(operationKey).isTrue();
                assertThat(contentHasExample(requestBody.path("content")))
                        .as("request de %s deve possuir exemplo", operationKey)
                        .isTrue();
            }

            if (operationKey.contains("{id}")) {
                assertUuidPathParameter(operation, operationKey);
            }

            if (PUBLIC_OPERATIONS.contains(operationKey)) {
                JsonNode security = operation.path("security");
                assertThat(security.isMissingNode() || security.size() == 0)
                        .as("%s deve ser público", operationKey)
                        .isTrue();
            } else {
                assertThat(usesBearerAuth(operation))
                        .as("%s deve exigir bearerAuth", operationKey)
                        .isTrue();
            }
        }
    }

    @Test
    void shouldExposeOnlyDescribedPublicDtosAsSchemas() throws Exception {
        JsonNode schemas = loadOpenApiDocument()
                .path("components")
                .path("schemas");

        for (String schemaName : PUBLIC_SCHEMAS) {
            JsonNode schema = schemas.path(schemaName);

            assertThat(schema.isMissingNode())
                    .as("schema %s deve existir", schemaName)
                    .isFalse();
            assertThat(schema.path("description").asString())
                    .as("schema %s deve possuir descrição", schemaName)
                    .isNotBlank();

            for (var property : schema.path("properties").properties()) {
                assertThat(property.getValue().path("description").asString())
                        .as("propriedade %s.%s deve possuir descrição", schemaName, property.getKey())
                        .isNotBlank();
            }
        }

        for (String entityName : List.of(
                "Account",
                "Category",
                "Transaction",
                "User",
                "CreditCard",
                "Invoice",
                "Budget"
        )) {
            assertThat(schemas.path(entityName).isMissingNode())
                    .as("Entity %s não pode ser contrato público", entityName)
                    .isTrue();
        }
    }

    @Test
    void shouldDocumentCreditCardContractsAndKeepInternalOwnershipOutOfTheApi() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode create = findOperation(document, "post /api/v1/credit-cards");
        JsonNode list = findOperation(document, "get /api/v1/credit-cards");
        JsonNode find = findOperation(document, "get /api/v1/credit-cards/{id}");
        JsonNode update = findOperation(document, "patch /api/v1/credit-cards/{id}");

        assertThat(create.path("requestBody").path("required").asBoolean()).isTrue();
        assertThat(contentReferencesSchema(
                create.path("requestBody").path("content"),
                "CreateCreditCardRequest"
        )).isTrue();
        assertThat(contentReferencesSchema(
                create.path("responses").path("201").path("content"),
                "CreditCardResponse"
        )).isTrue();

        assertThat(list.path("responses").path("200")
                .path("content").path("application/json")
                .path("schema").path("type").asString()).isEqualTo("array");
        assertThat(list.path("responses").path("200")
                .path("content").path("application/json")
                .path("schema").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/CreditCardResponse");

        assertUuidPathParameter(find, "get /api/v1/credit-cards/{id}");
        assertUuidPathParameter(update, "patch /api/v1/credit-cards/{id}");
        assertThat(contentReferencesSchema(
                update.path("requestBody").path("content"),
                "UpdateCreditCardRequest"
        )).isTrue();

        assertThat(schemas.path("CreditCardResponse").path("properties")
                .properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(
                        "id",
                        "name",
                        "creditLimit",
                        "availableLimit",
                        "closingDay",
                        "dueDay",
                        "defaultAccountId",
                        "status",
                        "version"
                )
                .doesNotContain("userId");

        assertThat(schemas.path("CreateCreditCardRequest").path("required")
                .valueStream().map(JsonNode::asString).toList())
                .containsExactlyInAnyOrder(
                        "name",
                        "creditLimit",
                        "closingDay",
                        "dueDay",
                        "defaultAccountId"
                );
        assertThat(schemas.path("UpdateCreditCardRequest")
                .path("properties").has("availableLimit")).isFalse();
        assertThat(schemas.path("UpdateCreditCardRequest")
                .path("properties").path("status").path("enum")
                .valueStream().map(JsonNode::asString).toList())
                .containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "BLOCKED");
        assertThat(schemas.path("CreditCard").isMissingNode()).isTrue();

        for (JsonNode operation : List.of(create, list, find, update)) {
            assertThat(usesBearerAuth(operation)).isTrue();
        }
        for (String statusCode : List.of("400", "401", "404", "409", "500")) {
            JsonNode response = update.path("responses").path(statusCode);
            assertThat(response.isMissingNode()).as(statusCode).isFalse();
            assertThat(contentReferencesSchema(
                    response.path("content"),
                    "ApiError"
            )).as(statusCode).isTrue();
        }
    }

    @Test
    void shouldDocumentTheCreditCardPurchaseContractAndStandardErrors() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode schemas = document.path("components").path("schemas");
        JsonNode purchase = findOperation(
                document,
                "post /api/v1/credit-cards/{id}/purchases"
        );

        assertUuidPathParameter(
                purchase,
                "post /api/v1/credit-cards/{id}/purchases"
        );
        assertThat(purchase.path("requestBody").path("required").asBoolean()).isTrue();
        assertThat(contentReferencesSchema(
                purchase.path("requestBody").path("content"),
                "CreateCreditCardPurchaseRequest"
        )).isTrue();
        JsonNode successSchema = purchase.path("responses")
                .path("201")
                .path("content")
                .path(MediaType.APPLICATION_JSON_VALUE)
                .path("schema");
        assertThat(successSchema.path("type").asString()).isEqualTo("array");
        assertThat(successSchema.path("items").path("$ref").asString())
                .endsWith("/TransactionResponse");
        assertThat(usesBearerAuth(purchase)).isTrue();

        assertThat(schemas.path("CreateCreditCardPurchaseRequest")
                .path("required")
                .valueStream()
                .map(JsonNode::asString)
                .toList())
                .containsExactlyInAnyOrder(
                        "description",
                        "amount",
                        "purchaseDate",
                        "categoryId",
                        "installmentCount"
                );

        JsonNode properties = schemas.path("CreateCreditCardPurchaseRequest")
                .path("properties");
        assertThat(properties.path("description").path("maxLength").asInt())
                .isEqualTo(255);
        assertThat(properties.path("amount").path("minimum").asText())
                .isEqualTo("0.01");
        assertThat(properties.path("purchaseDate").path("type").asString())
                .isEqualTo("string");
        assertThat(properties.path("purchaseDate").path("format").asString())
                .isEqualTo("date");
        assertThat(properties.path("categoryId").path("format").asString())
                .isEqualTo("uuid");
        assertThat(properties.path("installmentCount").path("type").asString())
                .isEqualTo("integer");
        assertThat(properties.path("installmentCount").path("minimum").asText())
                .isEqualTo("1");

        for (String statusCode : List.of("400", "401", "404", "409", "500")) {
            JsonNode response = purchase.path("responses").path(statusCode);
            assertThat(response.isMissingNode()).as(statusCode).isFalse();
            assertThat(contentReferencesSchema(
                    response.path("content"),
                    "ApiError"
            )).as(statusCode).isTrue();
        }
    }

    @Test
    void shouldDocumentAllTransactionSearchParametersWithoutExposingUserIdOrRequestBody() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/transactions");
        Map<String, JsonNode> parameters = new LinkedHashMap<>();

        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asString();
            assertThat(parameters.put(name, parameter)).as("parâmetro %s não deve se repetir", name).isNull();
            assertThat(parameter.path("in").asString()).as(name).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).as(name).isFalse();
            assertThat(parameter.path("description").asString()).as(name).isNotBlank();
        }

        assertThat(parameters).containsOnlyKeys(
                "startDate", "endDate", "categoryId", "accountId", "creditCardId",
                "type", "status", "minAmount", "maxAmount", "description", "page", "size", "sort"
        );
        assertThat(parameters).doesNotContainKeys("userId", "filters", "pageable");
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();

        for (String name : List.of("startDate", "endDate")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("string");
            assertThat(schema.path("format").asString()).as(name).isEqualTo("date");
        }
        for (String name : List.of("categoryId", "accountId", "creditCardId")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("string");
            assertThat(schema.path("format").asString()).as(name).isEqualTo("uuid");
        }
        for (String name : List.of("minAmount", "maxAmount")) {
            JsonNode schema = resolveSchema(document, parameters.get(name).path("schema"));
            assertThat(schema.path("type").asString()).as(name).isEqualTo("number");
        }

        assertEnumValues(document, parameters.get("type").path("schema"),
                Arrays.stream(TransactionType.values()).map(Enum::name).toList());
        assertEnumValues(document, parameters.get("status").path("schema"),
                Arrays.stream(TransactionStatus.values()).map(Enum::name).toList());
        assertThat(parameters.get("description").path("schema").path("type").asString()).isEqualTo("string");
        assertThat(parameters.get("page").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("page").path("schema").path("default").isNumber()).isTrue();
        assertThat(parameters.get("page").path("schema").path("default").asInt()).isZero();
        assertThat(parameters.get("size").path("schema").path("type").asString()).isEqualTo("integer");
        assertThat(parameters.get("size").path("schema").path("default").asInt()).isEqualTo(20);
        assertThat(parameters.get("sort").path("schema").path("type").asString()).isEqualTo("array");
        assertThat(parameters.get("sort").path("schema").path("items").path("type").asString()).isEqualTo("string");
    }

    @Test
    void shouldDocumentTransactionPageDtoExampleAndStandardErrors() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/transactions");
        JsonNode response = operation.path("responses").path("200")
                .path("content").path("application/json");

        assertThat(response.path("schema").path("$ref").asString())
                .isEqualTo("#/components/schemas/TransactionPageResponse");

        JsonNode properties = resolveSchema(document, response.path("schema")).path("properties");
        assertThat(properties.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("content", "page", "size", "totalElements", "totalPages", "first", "last");
        assertThat(properties.path("content").path("type").asString()).isEqualTo("array");
        assertThat(properties.path("content").path("items").path("$ref").asString())
                .isEqualTo("#/components/schemas/TransactionListItemResponse");

        JsonNode example = response.path("examples").path("Página vazia").path("value");
        assertThat(example.isObject()).isTrue();
        assertThat(example.path("content").isArray()).isTrue();
        assertThat(example.path("content").size()).isZero();
        assertThat(example.path("page").asInt()).isZero();
        assertThat(example.path("size").asInt()).isEqualTo(20);
        assertThat(example.path("totalElements").asLong()).isZero();
        assertThat(example.path("totalPages").asInt()).isZero();
        assertThat(example.path("first").asBoolean()).isTrue();
        assertThat(example.path("last").asBoolean()).isTrue();

        for (String code : List.of("400", "401", "500")) {
            JsonNode errorResponse = operation.path("responses").path(code);
            assertThat(errorResponse.path("description").asString()).as(code).isNotBlank();
            assertThat(contentReferencesSchema(errorResponse.path("content"), "ApiError")).as(code).isTrue();
        }
    }

    @Test
    void shouldDocumentCsvExportWithSharedFiltersAndUtf8Contract() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(
                document,
                "get /api/v1/exports/transactions.csv"
        );
        Map<String, JsonNode> parameters = new LinkedHashMap<>();

        for (JsonNode parameter : operation.path("parameters")) {
            String name = parameter.path("name").asString();
            assertThat(parameters.put(name, parameter))
                    .as("parâmetro %s não deve se repetir", name)
                    .isNull();
            assertThat(parameter.path("in").asString())
                    .as(name)
                    .isEqualTo("query");
        }

        assertThat(parameters).containsOnlyKeys(
                "startDate",
                "endDate",
                "categoryId",
                "accountId",
                "creditCardId",
                "type",
                "status",
                "minAmount",
                "maxAmount",
                "description"
        );
        assertThat(parameters).doesNotContainKeys(
                "userId",
                "page",
                "size",
                "sort"
        );
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();

        JsonNode csvContent = operation.path("responses")
                .path("200")
                .path("content")
                .path("text/csv");

        assertThat(csvContent.isMissingNode()).isFalse();
        assertThat(csvContent.path("schema").path("type").asString())
                .isEqualTo("string");
        assertThat(csvContent.path("examples")
                .path("Exportação CSV")
                .path("value")
                .asString())
                .startsWith("id,description,type,status,amount,competenceDate,effectiveDate,category,account")
                .contains("180.50", "2026-09-02");

        assertThat(operation.path("description").asString())
                .contains("UTF-8", "ISO-8601", "mesmos filtros");
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldDocumentCashReportsWithRequiredDateAndJwtWithoutUserIdOrRequestBody(String period) throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/reports/cash/" + period);
        JsonNode parameters = operation.path("parameters");

        assertThat(parameters.isArray()).isTrue();
        assertThat(parameters.size()).isEqualTo(1);
        JsonNode date = parameters.get(0);
        assertThat(date.path("name").asString()).isEqualTo("date");
        assertThat(date.path("in").asString()).isEqualTo("query");
        assertThat(date.path("required").asBoolean()).isTrue();
        assertThat(date.path("description").asString()).isNotBlank();
        JsonNode dateSchema = resolveSchema(document, date.path("schema"));
        assertThat(dateSchema.path("type").asString()).isEqualTo("string");
        assertThat(dateSchema.path("format").asString()).isEqualTo("date");
        String example = date.path("example").asString();
        if (example.isBlank()) example = dateSchema.path("example").asString();
        assertThat(example).isNotBlank();
        assertThat(LocalDate.parse(example)).isNotNull();
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();
        String expectedSchema = period.equals("daily") ? "DailyCashFlowResponse" : "WeeklyCashFlowResponse";
        assertThat(contentReferencesSchema(operation.path("responses").path("200").path("content"), expectedSchema))
                .isTrue();
    }

    @Test
    void shouldDocumentCompetenceReportPeriodSecurityAndCriticalAccountingRule() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation = findOperation(document, "get /api/v1/reports/competence");
        JsonNode parameters = operation.path("parameters");

        assertThat(parameters.isArray()).isTrue();
        assertThat(parameters).hasSize(2);
        assertThat(parameters.valueStream().map(parameter -> parameter.path("name").asString()).toList())
                .containsExactlyInAnyOrder("startDate", "endDate");

        for (JsonNode parameter : parameters) {
            assertThat(parameter.path("in").asString()).isEqualTo("query");
            assertThat(parameter.path("required").asBoolean()).isTrue();
            assertThat(parameter.path("description").asString()).isNotBlank();
            JsonNode schema = resolveSchema(document, parameter.path("schema"));
            assertThat(schema.path("type").asString()).isEqualTo("string");
            assertThat(schema.path("format").asString()).isEqualTo("date");
        }

        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();
        assertThat(contentReferencesSchema(
                operation.path("responses").path("200").path("content"),
                "CompetenceReportResponse"
        )).isTrue();
        assertThat(operation.path("description").asString())
                .contains("competenceDate", "CREDIT_CARD_PURCHASE", "CREDIT_CARD_PAYMENT");
    }

    @Test
    void shouldPublishConsistentCompetenceReportSchemaAndExample() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode schema = document.path("components").path("schemas").path("CompetenceReportResponse");
        JsonNode properties = schema.path("properties");

        assertThat(properties.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(
                        "startDate",
                        "endDate",
                        "totalIncome",
                        "totalExpenses",
                        "result",
                        "incomeCategories",
                        "expenseCategories"
                );
        for (String field : List.of("totalIncome", "totalExpenses", "result")) {
            assertThat(properties.path(field).path("type").asString()).isEqualTo("number");
        }
        for (String field : List.of("incomeCategories", "expenseCategories")) {
            assertThat(properties.path(field).path("type").asString()).isEqualTo("array");
            assertThat(properties.path(field).path("items").path("$ref").asString())
                    .isEqualTo("#/components/schemas/CategoryCashFlowResponse");
        }

        JsonNode example = findOperation(document, "get /api/v1/reports/competence")
                .path("responses")
                .path("200")
                .path("content")
                .path("application/json")
                .path("examples")
                .path("Relatório por competência")
                .path("value");

        assertThat(example.path("totalExpenses").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(example.path("result").decimalValue()).isEqualByComparingTo(
                example.path("totalIncome").decimalValue()
                        .subtract(example.path("totalExpenses").decimalValue())
        );
        assertThat(cashCategoryTotal(example.path("expenseCategories")))
                .isEqualByComparingTo(example.path("totalExpenses").decimalValue());
    }

    @Test
    void shouldPublishCashReportDtoFieldsAndReferencesWithoutEntitiesOrInternalProjections() throws Exception {
        JsonNode schemas = loadOpenApiDocument().path("components").path("schemas");
        Map<String, List<String>> fields = Map.of(
                "DailyCashFlowResponse", List.of("date", "summary"),
                "WeeklyCashFlowResponse", List.of("currentWeek", "previousWeek", "comparison"),
                "CashFlowPeriodResponse", List.of("startDate", "endDate", "summary"),
                "CashFlowSummaryResponse", List.of("inflows", "outflows", "net", "invoicePayments",
                        "incomeCategories", "expenseCategories"),
                "CashFlowComparisonResponse", List.of("inflowsDifference", "outflowsDifference", "netDifference"),
                "CategoryCashFlowResponse", List.of("categoryId", "name", "amount")
        );
        for (var expected : fields.entrySet()) {
            assertThat(schemas.path(expected.getKey()).path("properties").properties().stream().map(Map.Entry::getKey).toList())
                    .as(expected.getKey()).containsExactlyInAnyOrderElementsOf(expected.getValue());
        }
        JsonNode summary = schemas.path("CashFlowSummaryResponse").path("properties");
        for (String field : List.of("inflows", "outflows", "net", "invoicePayments")) {
            assertThat(summary.path(field).path("type").asString()).as(field).isEqualTo("number");
        }
        for (String field : List.of("incomeCategories", "expenseCategories")) {
            assertThat(summary.path(field).path("type").asString()).isEqualTo("array");
            assertThat(summary.path(field).path("items").path("$ref").asString())
                    .isEqualTo("#/components/schemas/CategoryCashFlowResponse");
        }
        assertThat(schemas.path("DailyCashFlowResponse").path("properties").path("summary").path("$ref").asString())
                .isEqualTo("#/components/schemas/CashFlowSummaryResponse");
        assertThat(schemas.path("CashFlowPeriodResponse").path("properties").path("summary").path("$ref").asString())
                .isEqualTo("#/components/schemas/CashFlowSummaryResponse");
        JsonNode weekly = schemas.path("WeeklyCashFlowResponse").path("properties");
        for (String field : List.of("currentWeek", "previousWeek")) {
            assertThat(weekly.path(field).path("$ref").asString()).isEqualTo("#/components/schemas/CashFlowPeriodResponse");
        }
        assertThat(weekly.path("comparison").path("$ref").asString())
                .isEqualTo("#/components/schemas/CashFlowComparisonResponse");
        assertThat(schemas.path("CategoryCashFlowResponse").path("properties").path("categoryId").path("format").asString())
                .isEqualTo("uuid");
        assertThat(schemas.path("CashFlowAggregate").isMissingNode()).isTrue();
        assertThat(schemas.path("Transaction").isMissingNode()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldProvideCashReportSuccessExamplesWithConsistentMoneyAndWeekComparisons(String period) throws Exception {
        JsonNode operation = findOperation(loadOpenApiDocument(), "get /api/v1/reports/cash/" + period);
        JsonNode examples = operation.path("responses").path("200").path("content")
                .path("application/json").path("examples");
        assertThat(examples.size()).isPositive();

        for (var entry : examples.properties()) {
            JsonNode example = entry.getValue().path("value");
            assertThat(example.isObject()).as(entry.getKey()).isTrue();
            if (period.equals("daily")) {
                assertThat(LocalDate.parse(example.path("date").asString())).isNotNull();
                assertCashSummaryExample(example.path("summary"));
            } else {
                JsonNode current = example.path("currentWeek");
                JsonNode previous = example.path("previousWeek");
                for (JsonNode week : List.of(current, previous)) {
                    LocalDate start = LocalDate.parse(week.path("startDate").asString());
                    assertThat(start.getDayOfWeek()).isEqualTo(java.time.DayOfWeek.MONDAY);
                    assertThat(LocalDate.parse(week.path("endDate").asString())).isEqualTo(start.plusDays(6));
                    assertCashSummaryExample(week.path("summary"));
                }
                assertThat(LocalDate.parse(previous.path("endDate").asString()).plusDays(1))
                        .isEqualTo(LocalDate.parse(current.path("startDate").asString()));
                for (String field : List.of("inflows", "outflows", "net")) {
                    JsonNode difference = example.path("comparison").path(field + "Difference");
                    assertThat(difference.isNumber()).isTrue();
                    assertThat(difference.decimalValue()).isEqualByComparingTo(
                            current.path("summary").path(field).decimalValue()
                                    .subtract(previous.path("summary").path(field).decimalValue()));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"daily", "weekly"})
    void shouldDocumentCashReportErrorsWithApiErrorAndEndpointSpecificExamples(String period) throws Exception {
        String path = "/api/v1/reports/cash/" + period;
        JsonNode operation = findOperation(loadOpenApiDocument(), "get " + path);
        List<String> badRequestCodes = new ArrayList<>();

        for (String statusCode : List.of("400", "401", "500")) {
            JsonNode response = operation.path("responses").path(statusCode);
            assertThat(response.path("description").asString()).as(statusCode).isNotBlank();
            assertThat(contentReferencesSchema(response.path("content"), "ApiError")).isTrue();
            JsonNode examples = response.path("content").path("application/json").path("examples");
            assertThat(examples.size()).as(statusCode).isPositive();
            for (var entry : examples.properties()) {
                JsonNode error = entry.getValue().path("value");
                assertThat(error.isObject()).isTrue();
                assertThat(error.properties().stream().map(Map.Entry::getKey).toList())
                        .containsExactlyInAnyOrder("timestamp", "status", "code", "message", "path", "fieldErrors");
                assertThat(error.path("status").asInt()).isEqualTo(Integer.parseInt(statusCode));
                assertThat(error.path("path").asString()).isEqualTo(path);
                assertThat(Instant.parse(error.path("timestamp").asString())).isNotNull();
                assertThat(error.path("message").asString()).isNotBlank();
                assertThat(error.path("fieldErrors").isArray()).isTrue();
                if (statusCode.equals("400")) badRequestCodes.add(error.path("code").asString());
                else assertThat(error.path("code").asString()).isEqualTo(
                        statusCode.equals("401") ? "UNAUTHORIZED" : "INTERNAL_SERVER_ERROR");
            }
        }
        assertThat(badRequestCodes).contains("VALIDATION_ERROR", "INVALID_REPORT_PERIOD");
    }

    @Test
    void shouldDocumentDashboardContractAndAccountingBasis() throws Exception {
        JsonNode document = loadOpenApiDocument();
        JsonNode operation =
                findOperation(document, "get /api/v1/dashboard");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.path("parameters").size()).isZero();
        assertThat(operation.path("requestBody").isMissingNode()).isTrue();
        assertThat(usesBearerAuth(operation)).isTrue();

        assertThat(contentReferencesSchema(
                operation.path("responses")
                        .path("200")
                        .path("content"),
                "DashboardResponse"
        )).isTrue();

        assertThat(operation.path("description").asString())
                .contains(
                        "effectiveDate",
                        "competenceDate",
                        "Compras no cartão",
                        "pagamentos de fatura",
                        "OPEN",
                        "CASH_AND_INVOICE",
                        "basis"
                );

        JsonNode properties = document
                .path("components")
                .path("schemas")
                .path("DashboardResponse")
                .path("properties");

        assertThat(
                properties.properties()
                        .stream()
                        .map(Map.Entry::getKey)
                        .toList()
        ).containsExactlyInAnyOrder(
                "referenceDate",
                "year",
                "month",
                "periodStart",
                "periodEnd",
                "monthlyBalance",
                "monthlyInflows",
                "totalOutflows",
                "monthlyOutflows",
                "creditCardPurchaseOutflows",
                "competenceExpenses",
                "openInvoices",
                "monthlyOpenInvoices",
                "budget",
                "consolidatedBalance"
        );
    }

    private void assertCashSummaryExample(JsonNode summary) {
        assertThat(summary.properties().stream().map(Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder("inflows", "outflows", "net", "invoicePayments", "incomeCategories", "expenseCategories");
        for (String field : List.of("inflows", "outflows", "net", "invoicePayments")) {
            assertThat(summary.path(field).isNumber()).as(field).isTrue();
        }
        assertThat(summary.path("net").decimalValue()).isEqualByComparingTo(
                summary.path("inflows").decimalValue().subtract(summary.path("outflows").decimalValue()));
        assertThat(cashCategoryTotal(summary.path("incomeCategories")))
                .isEqualByComparingTo(summary.path("inflows").decimalValue());
        assertThat(cashCategoryTotal(summary.path("expenseCategories")).add(summary.path("invoicePayments").decimalValue()))
                .isEqualByComparingTo(summary.path("outflows").decimalValue());
    }

    private BigDecimal cashCategoryTotal(JsonNode categories) {
        assertThat(categories.isArray()).isTrue();
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode category : categories) {
            assertThat(category.path("amount").isNumber()).isTrue();
            assertThat(category.path("name").asString()).isNotBlank();
            total = total.add(category.path("amount").decimalValue());
        }
        return total;
    }

    private JsonNode resolveSchema(JsonNode document, JsonNode schema) {
        String reference = schema.path("$ref").asString();
        if (reference.startsWith("#/")) {
            JsonNode resolved = document.at(reference.substring(1));
            assertThat(resolved.isMissingNode()).as(reference).isFalse();
            return resolved;
        }
        return schema;
    }

    private void assertEnumValues(JsonNode document, JsonNode schema, List<String> expected) {
        List<String> actual = new ArrayList<>();
        for (JsonNode value : resolveSchema(document, schema).path("enum")) {
            actual.add(value.asString());
        }
        assertThat(actual).containsExactlyInAnyOrderElementsOf(expected);
    }

    private JsonNode loadOpenApiDocument() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(json);
    }

    private JsonNode findOperation(JsonNode document, String operationKey) {
        String[] parts = operationKey.split(" ", 2);
        return document.path("paths").path(parts[1]).path(parts[0]);
    }

    private int countOperations(JsonNode paths) {
        Set<String> methods = Set.of("get", "post", "put", "patch", "delete");
        int count = 0;

        for (var path : paths.properties()) {
            for (var operation : path.getValue().properties()) {
                if (methods.contains(operation.getKey())) {
                    count++;
                }
            }
        }

        return count;
    }

    private void assertSuccessResponseHasExample(JsonNode operation, String operationKey) {
        String successCode = null;
        JsonNode successResponse = null;

        for (var response : operation.path("responses").properties()) {
            if (response.getKey().startsWith("2")) {
                successCode = response.getKey();
                successResponse = response.getValue();
                break;
            }
        }

        assertThat(successResponse)
                .as("%s deve documentar resposta de sucesso", operationKey)
                .isNotNull();

        if ("204".equals(successCode)) {
            JsonNode content = successResponse.path("content");

            assertThat(content.isMissingNode() || content.size() == 0)
                    .as("resposta 204 de %s não deve possuir corpo", operationKey)
                    .isTrue();

            return;
        }

        assertThat(contentHasExample(successResponse.path("content")))
                .as("resposta de sucesso de %s deve possuir exemplo", operationKey)
                .isTrue();
    }

    private void assertErrorResponsesUseApiError(JsonNode operation, String operationKey) {
        boolean foundErrorResponse = false;

        for (var response : operation.path("responses").properties()) {
            if (response.getKey().startsWith("4") || response.getKey().startsWith("5")) {
                foundErrorResponse = true;
                assertThat(contentReferencesSchema(response.getValue().path("content"), "ApiError"))
                        .as("erro %s de %s deve usar ApiError", response.getKey(), operationKey)
                        .isTrue();
            }
        }

        assertThat(foundErrorResponse)
                .as("%s deve documentar respostas de erro", operationKey)
                .isTrue();
    }

    private void assertUuidPathParameter(JsonNode operation, String operationKey) {
        JsonNode idParameter = null;

        for (JsonNode parameter : operation.path("parameters")) {
            if ("id".equals(parameter.path("name").asString())
                    && "path".equals(parameter.path("in").asString())) {
                idParameter = parameter;
                break;
            }
        }

        assertThat(idParameter)
                .as("%s deve documentar o UUID do path", operationKey)
                .isNotNull();
        assertThat(idParameter.path("required").asBoolean()).isTrue();
        assertThat(idParameter.path("description").asString()).isNotBlank();
        assertThat(idParameter.path("example").asString()).isNotBlank();
        assertThat(idParameter.path("schema").path("format").asString()).isEqualTo("uuid");
    }

    private boolean usesBearerAuth(JsonNode operation) {
        for (JsonNode requirement : operation.path("security")) {
            if (requirement.path("bearerAuth").isArray()) {
                return true;
            }
        }

        return false;
    }

    private boolean contentHasExample(JsonNode content) {
        for (var mediaType : content.properties()) {
            JsonNode media = mediaType.getValue();
            JsonNode example = media.path("example");
            JsonNode examples = media.path("examples");

            if ((!example.isMissingNode() && !example.isNull())
                    || (examples.isObject() && examples.size() > 0)) {
                return true;
            }
        }

        return false;
    }

    private boolean contentReferencesSchema(JsonNode content, String schemaName) {
        for (var mediaType : content.properties()) {
            String reference = mediaType.getValue()
                    .path("schema")
                    .path("$ref")
                    .asString();

            if (reference.endsWith("/" + schemaName)) {
                return true;
            }
        }

        return false;
    }
}
