package com.amorim.finance_manager;

import com.amorim.finance_manager.account.entity.Account;
import com.amorim.finance_manager.account.entity.AccountStatus;
import com.amorim.finance_manager.account.entity.AccountType;
import com.amorim.finance_manager.account.repository.AccountRepository;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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
