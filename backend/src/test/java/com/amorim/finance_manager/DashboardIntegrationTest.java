package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.bill.entity.Bill;
import com.amorim.finance_manager.bill.entity.BillStatus;
import com.amorim.finance_manager.bill.repository.BillRepository;
import com.amorim.finance_manager.budget.entity.Budget;
import com.amorim.finance_manager.budget.repository.BudgetRepository;
import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.entity.CategoryStatus;
import com.amorim.finance_manager.category.entity.CategoryType;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.creditcard.entity.CreditCard;
import com.amorim.finance_manager.creditcard.entity.CreditCardStatus;
import com.amorim.finance_manager.creditcard.repository.CreditCardRepository;
import com.amorim.finance_manager.invoice.entity.Invoice;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.security.JwtService;
import com.amorim.finance_manager.transaction.entity.PaymentMethod;
import com.amorim.finance_manager.transaction.entity.Transaction;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.transaction.repository.TransactionRepository;
import com.amorim.finance_manager.user.entity.User;
import com.amorim.finance_manager.user.repository.UserRepository;
import com.amorim.finance_manager.user.service.CustomUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@Transactional
class DashboardIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-15T12:00:00Z");
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @MockitoBean(name = "financeClock")
    private Clock financeClock;

    @BeforeEach
    void stubClock() {
        when(financeClock.instant()).thenReturn(NOW);
        when(financeClock.getZone()).thenReturn(ZONE);
    }

    @Test
    void shouldReturnAllDashboardIndicatorsWithTheirAccountingBasis() throws Exception {
        TestUser owner = createUserWithFinancialStructure("owner", "5000.00");
        UUID septemberInvoiceId = saveInvoice(owner.creditCardId(), 9, InvoiceStatus.OPEN, "700.00");

        saveTransaction(owner, TransactionType.INCOME, "500.00",
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 1));
        saveTransaction(owner, TransactionType.INCOME, "3000.00",
                LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 5));
        saveTransaction(owner, TransactionType.EXPENSE, "300.00",
                LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 6));
        saveTransaction(owner, TransactionType.CREDIT_CARD_PURCHASE, "700.00",
                LocalDate.of(2026, 8, 15), null, septemberInvoiceId);
        saveTransaction(owner, TransactionType.CREDIT_CARD_PAYMENT, "500.00",
                LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 15));

        saveInvoice(owner.creditCardId(), 8, InvoiceStatus.OPEN, "200.00");
        saveInvoice(owner.creditCardId(), 7, InvoiceStatus.PAID, "100.00");

        Budget savedBudget = new Budget();
        savedBudget.setUserId(owner.id());
        savedBudget.setCategoryId(owner.expenseCategoryId());
        savedBudget.setMonth(9);
        savedBudget.setYear(2026);
        savedBudget.setAmountLimit(new BigDecimal("1000.00"));
        budgetRepository.saveAndFlush(savedBudget);

        // Valores de outro mês e de outro usuário não podem contaminar o dashboard.
        saveTransaction(owner, TransactionType.EXPENSE, "9000.00",
                LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 20));
        TestUser anotherUser = createUserWithFinancialStructure("another", "9999.00");
        saveTransaction(anotherUser, TransactionType.INCOME, "8000.00",
                LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 10));
        saveInvoice(anotherUser.creditCardId(), 9, InvoiceStatus.OPEN, "5000.00");

        JsonNode response = getDashboard(owner.token());

        assertThat(response.properties().stream().map(java.util.Map.Entry::getKey).toList())
                .containsExactlyInAnyOrder(
                        "referenceDate", "year", "month", "periodStart", "periodEnd",
                        "monthlyBalance", "monthlyInflows", "totalOutflows",
                        "monthlyOutflows", "creditCardPurchaseOutflows",
                        "competenceExpenses", "openInvoices", "monthlyOpenInvoices", "budget", "consolidatedBalance"
                );
        assertThat(response.path("referenceDate").asText()).isEqualTo("2026-09-15");
        assertThat(response.path("year").asInt()).isEqualTo(2026);
        assertThat(response.path("month").asInt()).isEqualTo(9);
        assertThat(response.path("periodStart").asText()).isEqualTo("2026-09-01");
        assertThat(response.path("periodEnd").asText()).isEqualTo("2026-09-30");

        assertIndicator(response, "monthlyBalance", "2700.00", "CASH");
        assertIndicator(response, "monthlyInflows", "3500.00", "CASH");
        assertIndicator(response, "totalOutflows", "9800.00", "CASH");
        assertIndicator(response, "monthlyOutflows", "800.00", "CASH");
        assertIndicator(response, "creditCardPurchaseOutflows", "700.00", "COMPETENCE");
        assertIndicator(response, "competenceExpenses", "300.00", "COMPETENCE");
        assertIndicator(response, "openInvoices", "900.00", "COMPETENCE");
        assertIndicator(response, "monthlyOpenInvoices", "700.00", "COMPETENCE");
        assertIndicator(response, "consolidatedBalance", "5000.00", "CASH");

        JsonNode budget = response.path("budget");
        assertThat(budget.path("basis").asText()).isEqualTo("COMPETENCE");
        assertThat(budget.path("totalLimit").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(budget.path("totalSpent").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(budget.path("usagePercentage").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(budget.path("items")).hasSize(1);

        JsonNode item = budget.path("items").get(0);
        assertThat(item.path("budgetId").asText()).isNotBlank();
        assertThat(item.path("categoryId").asText()).isEqualTo(owner.expenseCategoryId().toString());
        assertThat(item.path("amountLimit").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(item.path("spentAmount").decimalValue()).isEqualByComparingTo("300.00");
        assertThat(item.path("usagePercentage").decimalValue()).isEqualByComparingTo("30.00");
        assertThat(item.path("alertStatus").asText()).isEqualTo("NORMAL");
    }

    @Test
    void shouldReturnZeroedDashboardWhenUserHasNoFinancialData() throws Exception {
        String token = createBareUser("empty").token();

        JsonNode response = getDashboard(token);

        assertIndicator(response, "monthlyBalance", "0.00", "CASH");
        assertIndicator(response, "monthlyInflows", "0.00", "CASH");
        assertIndicator(response, "totalOutflows", "0.00", "CASH");
        assertIndicator(response, "monthlyOutflows", "0.00", "CASH");
        assertIndicator(response, "creditCardPurchaseOutflows", "0.00", "COMPETENCE");
        assertIndicator(response, "competenceExpenses", "0.00", "COMPETENCE");
        assertIndicator(response, "openInvoices", "0.00", "COMPETENCE");
        assertIndicator(response, "monthlyOpenInvoices", "0.00", "COMPETENCE");
        assertIndicator(response, "consolidatedBalance", "0.00", "CASH");

        JsonNode budget = response.path("budget");
        assertThat(budget.path("basis").asText()).isEqualTo("COMPETENCE");
        assertThat(budget.path("totalLimit").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(budget.path("totalSpent").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(budget.path("usagePercentage").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(budget.path("items")).isEmpty();
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldSumOpenInvoicesByDueDateRegardlessOfReferenceMonth() throws Exception {
        TestUser owner = createUserWithFinancialStructure("walter", "0.00");
        saveInvoiceWithDueDate(owner.creditCardId(), 8, InvoiceStatus.OPEN, "100.00",
                LocalDate.of(2026, 9, 1));
        saveInvoiceWithDueDate(owner.creditCardId(), 7, InvoiceStatus.OPEN, "200.00",
                LocalDate.of(2026, 9, 30));
        // September reference, October due date: not part of September's monthly card.
        saveInvoiceWithDueDate(owner.creditCardId(), 9, InvoiceStatus.OPEN, "900.00",
                LocalDate.of(2026, 10, 1));
        saveInvoiceWithDueDate(owner.creditCardId(), 6, InvoiceStatus.OPEN, "50.00",
                LocalDate.of(2026, 8, 31));
        saveInvoiceWithDueDate(owner.creditCardId(), 5, InvoiceStatus.PAID, "400.00",
                LocalDate.of(2026, 9, 10));
        saveInvoiceWithDueDate(owner.creditCardId(), 4, InvoiceStatus.CANCELLED, "500.00",
                LocalDate.of(2026, 9, 12));
        TestUser anotherUser = createUserWithFinancialStructure("jesse", "0.00");
        saveInvoiceWithDueDate(anotherUser.creditCardId(), 8, InvoiceStatus.OPEN, "5000.00",
                LocalDate.of(2026, 9, 15));

        JsonNode response = getDashboard(owner.token());

        assertIndicator(response, "monthlyOpenInvoices", "300.00", "COMPETENCE");
        assertIndicator(response, "openInvoices", "1250.00", "COMPETENCE");
    }

    @Test
    void shouldIncludePreviousYearInvoiceWhenDueInJanuary() throws Exception {
        when(financeClock.instant()).thenReturn(Instant.parse("2027-01-15T12:00:00Z"));
        TestUser owner = createUserWithFinancialStructure("saul", "0.00");
        saveInvoiceWithDueDate(owner.creditCardId(), 12, InvoiceStatus.OPEN, "275.00",
                LocalDate.of(2027, 1, 17));

        assertIndicator(getDashboard(owner.token()), "monthlyOpenInvoices", "275.00", "COMPETENCE");
    }

    private void saveInvoiceWithDueDate(UUID cardId, int referenceMonth, InvoiceStatus status,
                                        String amount, LocalDate dueDate) {
        UUID id = saveInvoice(cardId, referenceMonth, status, amount);
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();
        invoice.setDueDate(dueDate);
        invoiceRepository.saveAndFlush(invoice);
    }

    @ParameterizedTest
    @CsvSource({"2026-09-10, 250.00, 0.00", "2026-08-10, 0.00, 250.00"})
    void shouldProjectPendingBillsAndReplaceThemWithPaymentWithoutDoubleCounting(
            String paymentDate, String septemberPayment, String augustPayment) throws Exception {
        TestUser owner = createUserWithFinancialStructure("walter", "1000.00");
        Bill bill = saveBill(owner, "250.00", LocalDate.of(2026, 9, 30), BillStatus.PENDING);
        saveBill(owner, "300.00", LocalDate.of(2026, 10, 30), BillStatus.PENDING);
        saveBill(owner, "600.00", LocalDate.of(2026, 9, 15), BillStatus.CANCELLED);
        TestUser anotherUser = createUserWithFinancialStructure("jesse", "0.00");
        saveBill(anotherUser, "5000.00", LocalDate.of(2026, 9, 15), BillStatus.PENDING);

        JsonNode before = getDashboard(owner.token());
        assertIndicator(before, "monthlyOutflows", "250.00", "CASH_AND_BILL");
        assertIndicator(before, "monthlyBalance", "-250.00", "CASH_AND_BILL");
        assertIndicator(before, "consolidatedBalance", "1000.00", "CASH");
        assertChartOutflows(owner, "250.00", "0.00", "250.00");

        mockMvc.perform(post("/api/v1/bills/{id}/pay", bill.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sourceAccountId":"%s","paymentDate":"%s","expectedVersion":%d}
                                """.formatted(owner.accountId(), paymentDate, bill.getVersion())))
                .andExpect(status().isOk());

        JsonNode after = getDashboard(owner.token());
        assertIndicator(after, "monthlyOutflows", septemberPayment, "CASH");
        assertIndicator(after, "monthlyBalance", new BigDecimal(septemberPayment).negate().toPlainString(), "CASH");
        assertIndicator(after, "consolidatedBalance", "750.00", "CASH");
        assertChartOutflows(owner, septemberPayment, augustPayment, septemberPayment);
    }

    private Bill saveBill(TestUser owner, String amount, LocalDate dueDate, BillStatus status) {
        Bill bill = new Bill();
        bill.setUserId(owner.id());
        bill.setCategoryId(owner.expenseCategoryId());
        bill.setDescription("Financiamento de Walter");
        bill.setAmount(new BigDecimal(amount));
        bill.setDueDate(dueDate);
        bill.setSeriesId(UUID.randomUUID());
        bill.setInstallmentNumber(1);
        bill.setInstallmentCount(1);
        bill.setStatus(status);
        return billRepository.saveAndFlush(bill);
    }

    private void assertChartOutflows(TestUser owner, String monthly, String august, String september) throws Exception {
        String content = mockMvc.perform(get("/api/v1/dashboard/charts/monthly")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token())
                        .param("year", "2026").param("month", "9"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode chart = objectMapper.readTree(content);
        assertThat(chart.path("totalExpenses").decimalValue()).isEqualByComparingTo(monthly);
        if (new BigDecimal(monthly).signum() > 0) {
            assertThat(chart.path("expenseCategories")).hasSize(1);
            assertThat(chart.path("expenseCategories").get(0).path("categoryId").asText())
                    .isEqualTo(owner.expenseCategoryId().toString());
        } else {
            assertThat(chart.path("expenseCategories")).isEmpty();
        }
        content = mockMvc.perform(get("/api/v1/dashboard/charts/annual")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.token()).param("year", "2026"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode evolution = objectMapper.readTree(content).path("evolution");
        assertThat(evolution.get(7).path("totals").path("outflows").decimalValue()).isEqualByComparingTo(august);
        assertThat(evolution.get(8).path("totals").path("outflows").decimalValue()).isEqualByComparingTo(september);
        assertThat(evolution.get(9).path("totals").path("outflows").decimalValue()).isEqualByComparingTo("300.00");
    }

    private JsonNode getDashboard(String token) throws Exception {
        String content = mockMvc.perform(get("/api/v1/dashboard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        return objectMapper.readTree(content);
    }

    private TestUser createUserWithFinancialStructure(String prefix, String currentBalance) {
        TestUser user = createBareUser(prefix);

        Account account = new Account();
        account.setUserId(user.id());
        account.setName(prefix + " checking");
        account.setType(AccountType.CHECKING);
        account.setInstitution("Bank");
        account.setInitialBalance(new BigDecimal(currentBalance));
        account.setCurrentBalance(new BigDecimal(currentBalance));
        account.setStatus(AccountStatus.ACTIVE);
        account = accountRepository.saveAndFlush(account);

        Category incomeCategory = new Category();
        incomeCategory.setUserId(user.id());
        incomeCategory.setName(prefix + " income");
        incomeCategory.setType(CategoryType.INCOME);
        incomeCategory.setStatus(CategoryStatus.ACTIVE);
        incomeCategory = categoryRepository.saveAndFlush(incomeCategory);

        Category expenseCategory = new Category();
        expenseCategory.setUserId(user.id());
        expenseCategory.setName(prefix + " expense");
        expenseCategory.setType(CategoryType.EXPENSE);
        expenseCategory.setStatus(CategoryStatus.ACTIVE);
        expenseCategory = categoryRepository.saveAndFlush(expenseCategory);

        CreditCard creditCard = new CreditCard();
        creditCard.setUserId(user.id());
        creditCard.setName(prefix + " card");
        creditCard.setCreditLimit(new BigDecimal("10000.00"));
        creditCard.setAvailableLimit(new BigDecimal("10000.00"));
        creditCard.setClosingDay(10);
        creditCard.setDueDay(17);
        creditCard.setDefaultAccountId(account.getId());
        creditCard.setStatus(CreditCardStatus.ACTIVE);
        creditCard = creditCardRepository.saveAndFlush(creditCard);

        return new TestUser(
                user.id(), user.token(), account.getId(), incomeCategory.getId(),
                expenseCategory.getId(), creditCard.getId()
        );
    }

    private TestUser createBareUser(String prefix) {
        User user = new User();
        user.setName(prefix + " user");
        user.setEmail(prefix + "." + UUID.randomUUID() + "@example.com");
        user.setPasswordHash(passwordEncoder.encode("StrongPass123!"));
        User saved = userRepository.saveAndFlush(user);

        String token = jwtService.generateToken(userDetailsService.loadUserByUsername(saved.getEmail()));
        return new TestUser(saved.getId(), token, null, null, null, null);
    }

    private void saveTransaction(
            TestUser owner,
            TransactionType type,
            String amount,
            LocalDate competenceDate,
            LocalDate effectiveDate
    ) {
        saveTransaction(owner, type, amount, competenceDate, effectiveDate, null);
    }

    private void saveTransaction(
            TestUser owner,
            TransactionType type,
            String amount,
            LocalDate competenceDate,
            LocalDate effectiveDate,
            UUID invoiceId
    ) {
        Transaction transaction = new Transaction();
        transaction.setUserId(owner.id());
        transaction.setDescription(type.name().toLowerCase());
        transaction.setAmount(new BigDecimal(amount));
        transaction.setCompetenceDate(competenceDate);
        transaction.setEffectiveDate(effectiveDate);
        transaction.setDueDate(competenceDate);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.COMPLETED);
        transaction.setPaymentMethod(PaymentMethod.PIX);

        switch (type) {
            case INCOME -> {
                transaction.setDestinationAccountId(owner.accountId());
                transaction.setCategoryId(owner.incomeCategoryId());
            }
            case EXPENSE -> {
                transaction.setSourceAccountId(owner.accountId());
                transaction.setCategoryId(owner.expenseCategoryId());
            }
            case CREDIT_CARD_PURCHASE -> {
                transaction.setPaymentMethod(PaymentMethod.CREDIT_CARD);
                transaction.setCategoryId(owner.expenseCategoryId());
                transaction.setCreditCardId(owner.creditCardId());
                transaction.setInvoiceId(invoiceId);
            }
            case CREDIT_CARD_PAYMENT -> transaction.setSourceAccountId(owner.accountId());
            default -> throw new IllegalArgumentException("Unsupported transaction type: " + type);
        }

        transactionRepository.saveAndFlush(transaction);
    }

    private UUID saveInvoice(UUID creditCardId, int month, InvoiceStatus status, String totalAmount) {
        Invoice invoice = new Invoice();
        invoice.setCreditCardId(creditCardId);
        invoice.setReferenceMonth(month);
        invoice.setReferenceYear(2026);
        invoice.setClosingDate(LocalDate.of(2026, month, 10));
        invoice.setDueDate(LocalDate.of(2026, month, 17));
        invoice.setTotalAmount(new BigDecimal(totalAmount));
        invoice.setStatus(status);

        if (status == InvoiceStatus.PAID) {
            invoice.setPaidAt(NOW);
        }

        return invoiceRepository.saveAndFlush(invoice).getId();
    }

    private void assertIndicator(JsonNode response, String field, String amount, String basis) {
        JsonNode indicator = response.path(field);
        assertThat(indicator.path("amount").decimalValue()).isEqualByComparingTo(amount);
        assertThat(indicator.path("basis").asText()).isEqualTo(basis);
    }

    private record TestUser(
            UUID id,
            String token,
            UUID accountId,
            UUID incomeCategoryId,
            UUID expenseCategoryId,
            UUID creditCardId
    ) {
    }
}
