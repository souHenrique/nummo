package com.amorim.finance_manager;

import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.bill.repository.BillRepository;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
class BillIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BillRepository bills;
    @Autowired TransactionRepository transactions;
    @Autowired AccountRepository accounts;
    @Autowired CategoryRepository categories;
    @Autowired UserRepository users;
    String token;
    String categoryId;
    String accountId;

    @BeforeEach
    void setUp() throws Exception {
        token = registerAndLogin("Jesse Pinkman");
        categoryId = body(mvc.perform(post("/api/v1/categories").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Financiamento\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
        accountId = body(mvc.perform(post("/api/v1/accounts").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Conta de Jesse\",\"type\":\"CHECKING\",\"initialBalance\":1000}"))
                .andExpect(status().isCreated()).andReturn()).get("id").asText();
    }

    @AfterEach
    void clean() {
        bills.deleteAll();
        transactions.deleteAll();
        accounts.deleteAll();
        categories.deleteAll();
        users.deleteAll();
    }

    @Test
    void createsMonthlyInstallmentsAndFiltersByDueMonth() throws Exception {
        JsonNode created = create(36, "2026-01-31");
        assertThat(created.size()).isEqualTo(36);
        assertThat(created.get(1).get("dueDate").asText()).isEqualTo("2026-02-28");
        assertThat(created.get(2).get("dueDate").asText()).isEqualTo("2026-03-31");
        mvc.perform(get("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("year", "2026").param("month", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].installmentNumber").value(2));
        assertThat(transactions.count()).isZero();
        assertBalance("1000.00");
    }

    @Test
    void paymentDebitsAccountOnceAndAppearsInReportsWithoutInvoice() throws Exception {
        String id = create(1, "2026-09-30").get(0).get("id").asText();
        JsonNode paid = body(mvc.perform(post("/api/v1/bills/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(paymentBody(accountId)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID")).andReturn());
        assertBalance("899.75");
        assertThat(transactions.count()).isEqualTo(1);
        var transaction = transactions.findById(UUID.fromString(paid.get("paymentTransactionId").asText())).orElseThrow();
        assertThat(transaction.getInvoiceId()).isNull();
        assertThat(transaction.getCreditCardId()).isNull();
        assertThat(transaction.getEffectiveDate()).isEqualTo("2026-09-29");
        mvc.perform(get("/api/v1/reports/cash/monthly").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("year", "2026").param("month", "9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.summary.outflows").value(100.25))
                .andExpect(jsonPath("$.summary.invoiceOutflows").value(0));
        mvc.perform(post("/api/v1/bills/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(paymentBody(accountId)))
                .andExpect(status().isConflict());
        assertBalance("899.75");
        mvc.perform(post("/api/v1/transactions/{id}/cancel", transaction.getId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentPaymentsCannotDoubleDebit() throws Exception {
        String id = create(1, "2026-09-30").get(0).get("id").asText();
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> pay = () -> {
            start.await();
            return mvc.perform(post("/api/v1/bills/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON).content(paymentBody(accountId)))
                    .andReturn().getResponse().getStatus();
        };
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(pay);
            var second = executor.submit(pay);
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
        }
        assertThat(transactions.count()).isEqualTo(1);
        assertBalance("899.75");
    }

    @Test
    void foreignUsersCannotReadEditCancelPayOrListBills() throws Exception {
        String id = create(1, "2026-09-30").get(0).get("id").asText();
        String otherToken = registerAndLogin("Walter White");
        String auth = "Bearer " + otherToken;
        mvc.perform(get("/api/v1/bills/{id}", id).header(HttpHeaders.AUTHORIZATION, auth)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/v1/bills/{id}", id).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/bills/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(paymentBody(accountId))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/bills/{id}/cancel", id).header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, auth).param("year", "2026").param("month", "9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(post("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, auth)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(1, "2026-09-30")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsForeignPaymentAccountWithoutChangingBillOrBalance() throws Exception {
        String id = create(1, "2026-09-30").get(0).get("id").asText();
        mvc.perform(post("/api/v1/bills/{id}/pay", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(paymentBody(UUID.randomUUID().toString())))
                .andExpect(status().isNotFound());
        assertThat(bills.findById(UUID.fromString(id)).orElseThrow().getStatus().name()).isEqualTo("PENDING");
        assertThat(transactions.count()).isZero();
        assertBalance("1000.00");
    }

    @Test
    void editAndCancelAffectOnlySelectedInstallmentAndRejectStaleVersion() throws Exception {
        JsonNode created = create(3, "2026-09-30");
        String id = created.get(0).get("id").asText();
        mvc.perform(patch("/api/v1/bills/{id}", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(updateBody(0)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.amount").value(120.50));
        mvc.perform(post("/api/v1/bills/{id}/cancel", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":0}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/bills/{id}/cancel", id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"expectedVersion\":1}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertThat(bills.count()).isEqualTo(3);
        assertThat(bills.findById(UUID.fromString(created.get(1).get("id").asText())).orElseThrow().getAmount())
                .isEqualByComparingTo("100.25");
        assertThat(transactions.count()).isZero();
    }

    @Test
    void validatesInputAndRequiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(0, "2026-09-30")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(601, "2026-09-30")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(1, "2026-09-30").replace("100.25", "0")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/bills").param("year", "2026").param("month", "9"))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode create(int count, String date) throws Exception {
        return body(mvc.perform(post("/api/v1/bills").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(createBody(count, date)))
                .andExpect(status().isCreated()).andReturn());
    }
    private String createBody(int count, String date) {
        return """
                {"description":"Moto de Jesse","amount":100.25,"firstDueDate":"%s","installmentCount":%d,"categoryId":"%s"}
                """.formatted(date, count, categoryId);
    }
    private String paymentBody(String account) {
        return """
                {"sourceAccountId":"%s","paymentDate":"2026-09-29","expectedVersion":0}
                """.formatted(account);
    }
    private String updateBody(int version) {
        return """
                {"description":"Moto de Jesse","amount":120.50,"dueDate":"2026-09-30","categoryId":"%s","expectedVersion":%d}
                """.formatted(categoryId, version);
    }
    private JsonNode body(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString());
    }
    private void assertBalance(String expected) {
        assertThat(accounts.findById(UUID.fromString(accountId)).orElseThrow().getCurrentBalance()).isEqualByComparingTo(expected);
    }
    private String registerAndLogin(String name) throws Exception {
        String email = "bill." + UUID.randomUUID() + "@example.com";
        String password = "IntegrationPassword123!";
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"%s\"}".formatted(name, email, password)))
                .andExpect(status().isCreated());
        MvcResult result = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andReturn();
        return result.getResponse().getCookie("nummo_session").getValue();
    }
}
