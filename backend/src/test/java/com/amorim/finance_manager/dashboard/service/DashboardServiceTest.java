package com.amorim.finance_manager.dashboard.service;

import com.amorim.finance_manager.bill.entity.BillStatus;
import com.amorim.finance_manager.bill.repository.BillRepository;
import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.model.BudgetAlertStatus;
import com.amorim.finance_manager.budget.service.BudgetService;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.dashboard.model.AccountingBasis;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.dto.CashFlowSummaryResponse;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.MonthlyCashFlowResponse;
import com.amorim.finance_manager.report.service.CashFlowReportService;
import com.amorim.finance_manager.report.service.CompetenceReportService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID BUDGET_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private static final LocalDate START =
            LocalDate.of(2026, 9, 1);

    private static final LocalDate END =
            LocalDate.of(2026, 9, 30);

    @Mock
    private CashFlowReportService cashFlowReportService;

    @Mock
    private CompetenceReportService competenceReportService;

    @Mock
    private BudgetService budgetService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private BillRepository billRepository;

    @Mock
    private CurrentUserService currentUserService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        lenient().when(billRepository.sumAmountByDuePeriodAndStatus(any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        Clock clock = Clock.fixed(
                Instant.parse("2026-09-10T12:00:00Z"),
                ZoneId.of("America/Sao_Paulo")
        );

        dashboardService = new DashboardService(
                cashFlowReportService,
                competenceReportService,
                budgetService,
                accountRepository,
                invoiceRepository,
                billRepository,
                currentUserService,
                clock
        );
    }

    @ParameterizedTest
    @CsvSource({"0.00, 2200.00, 800.00, CASH", "250.00, 1950.00, 1050.00, CASH_AND_BILL",
            "3000.00, -800.00, 3800.00, CASH_AND_BILL"})
    void shouldAssembleDashboardUsingCashAndCompetenceRules(
            String pendingAmount, String expectedBalance, String expectedOutflows, AccountingBasis monthlyBasis) {
        when(billRepository.sumAmountByDuePeriodAndStatus(USER_ID, START, END, BillStatus.PENDING))
                .thenReturn(money(pendingAmount));
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(cashFlowReportService.monthly(2026, 9))
                .thenReturn(new MonthlyCashFlowResponse(
                        2026,
                        9,
                        START,
                        END,
                        new CashFlowSummaryResponse(
                                money("3000.00"),
                                money("800.00"),
                                money("2200.00"),
                                money("500.00"),
                                List.of(),
                                List.of()
                        )
                ));

        when(cashFlowReportService.totalOutflows())
                .thenReturn(money("9800.00"));

        when(competenceReportService.generate(START, END))
                .thenReturn(new CompetenceReportResponse(
                        START,
                        END,
                        money("3000.00"),
                        money("1000.00"),
                        money("2000.00"),
                        List.of(),
                        List.of()
                ));

        when(competenceReportService.creditCardPurchaseOutflows(2026, 9))
                .thenReturn(money("700.00"));

        when(accountRepository.sumCurrentBalanceByUserId(USER_ID))
                .thenReturn(money("5000.00"));

        when(invoiceRepository
                .sumTotalAmountOwnedByUserIdAndStatusIn(
                        USER_ID,
                        Set.of(InvoiceStatus.OPEN)
                ))
                .thenReturn(money("400.00"));

        when(invoiceRepository.sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID, START, END, Set.of(InvoiceStatus.OPEN)))
                .thenReturn(money("250.00"));

        BudgetResponse budget = new BudgetResponse(
                BUDGET_ID,
                CATEGORY_ID,
                9,
                2026,
                money("1000.00"),
                money("800.00"),
                money("80.00"),
                BudgetAlertStatus.ALERT,
                Instant.parse("2026-09-01T12:00:00Z"),
                Instant.parse("2026-09-10T12:00:00Z")
        );

        when(budgetService.findByPeriod(2026, 9))
                .thenReturn(List.of(budget));

        var response = dashboardService.get();

        assertThat(response.referenceDate())
                .isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(response.periodStart()).isEqualTo(START);
        assertThat(response.periodEnd()).isEqualTo(END);

        assertThat(response.monthlyBalance().basis())
                .isEqualTo(monthlyBasis);
        assertThat(response.monthlyBalance().amount())
                .isEqualByComparingTo(expectedBalance);

        assertThat(response.monthlyInflows().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.monthlyInflows().amount())
                .isEqualByComparingTo("3000.00");

        assertThat(response.totalOutflows().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.totalOutflows().amount())
                .isEqualByComparingTo("9800.00");

        assertThat(response.monthlyOutflows().basis())
                .isEqualTo(monthlyBasis);
        assertThat(response.monthlyOutflows().amount())
                .isEqualByComparingTo(expectedOutflows);

        assertThat(response.creditCardPurchaseOutflows().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.creditCardPurchaseOutflows().amount())
                .isEqualByComparingTo("700.00");

        assertThat(response.competenceExpenses().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.competenceExpenses().amount())
                .isEqualByComparingTo("1000.00");

        assertThat(response.openInvoices().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.openInvoices().amount())
                .isEqualByComparingTo("400.00");

        assertThat(response.monthlyOpenInvoices().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.monthlyOpenInvoices().amount())
                .isEqualByComparingTo("250.00");
        verify(invoiceRepository).sumTotalAmountOwnedByUserIdAndStatusIn(USER_ID, Set.of(InvoiceStatus.OPEN));
        verify(invoiceRepository).sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID, START, END, Set.of(InvoiceStatus.OPEN));
        verifyNoMoreInteractions(invoiceRepository);

        assertThat(response.consolidatedBalance().basis())
                .isEqualTo(AccountingBasis.CASH);
        assertThat(response.consolidatedBalance().amount())
                .isEqualByComparingTo("5000.00");

        assertThat(response.budget().basis())
                .isEqualTo(AccountingBasis.COMPETENCE);
        assertThat(response.budget().totalLimit())
                .isEqualByComparingTo("1000.00");
        assertThat(response.budget().totalSpent())
                .isEqualByComparingTo("800.00");
        assertThat(response.budget().usagePercentage())
                .isEqualByComparingTo("80.00");
        assertThat(response.budget().items()).singleElement()
                .satisfies(item -> {
                    assertThat(item.budgetId()).isEqualTo(BUDGET_ID);
                    assertThat(item.alertStatus())
                            .isEqualTo(BudgetAlertStatus.ALERT);
                });

        verify(cashFlowReportService).monthly(2026, 9);
        verify(cashFlowReportService).totalOutflows();
        verify(competenceReportService).generate(START, END);
        verify(competenceReportService).creditCardPurchaseOutflows(2026, 9);
        verify(budgetService).findByPeriod(2026, 9);
        verify(accountRepository).sumCurrentBalanceByUserId(USER_ID);
        verify(billRepository).sumAmountByDuePeriodAndStatus(USER_ID, START, END, BillStatus.PENDING);
        verifyNoMoreInteractions(billRepository, accountRepository);
    }

    @Test
    void shouldReturnZeroBudgetSummaryWithoutConfiguredBudgets() {
        when(currentUserService.getCurrentUserId())
                .thenReturn(USER_ID);

        when(cashFlowReportService.monthly(2026, 9))
                .thenReturn(new MonthlyCashFlowResponse(
                        2026,
                        9,
                        START,
                        END,
                        new CashFlowSummaryResponse(
                                money("0.00"),
                                money("0.00"),
                                money("0.00"),
                                money("0.00"),
                                List.of(),
                                List.of()
                        )
                ));

        when(cashFlowReportService.totalOutflows())
                .thenReturn(money("0.00"));

        when(competenceReportService.generate(START, END))
                .thenReturn(new CompetenceReportResponse(
                        START,
                        END,
                        money("0.00"),
                        money("0.00"),
                        money("0.00"),
                        List.of(),
                        List.of()
                ));

        when(competenceReportService.creditCardPurchaseOutflows(2026, 9))
                .thenReturn(money("0.00"));

        when(accountRepository.sumCurrentBalanceByUserId(USER_ID))
                .thenReturn(money("0.00"));

        when(invoiceRepository
                .sumTotalAmountOwnedByUserIdAndStatusIn(
                        USER_ID,
                        Set.of(InvoiceStatus.OPEN)
                ))
                .thenReturn(money("0.00"));

        when(budgetService.findByPeriod(2026, 9))
                .thenReturn(List.of());

        when(invoiceRepository.sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID, START, END, Set.of(InvoiceStatus.OPEN))).thenReturn(BigDecimal.ZERO);

        var response = dashboardService.get();

        assertThat(response.budget().totalLimit()).isZero();
        assertThat(response.budget().totalSpent()).isZero();
        assertThat(response.budget().usagePercentage()).isZero();
        assertThat(response.budget().items()).isEmpty();
        assertThat(response.monthlyOpenInvoices().amount()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"2026, 1, 31", "2026, 12, 31", "2028, 2, 29", "2026, 2, 28"})
    void shouldFilterMonthlyOpenInvoicesByCompleteDueDatePeriod(int year, int month, int lastDay) {
        YearMonth period = YearMonth.of(year, month);
        LocalDate start = period.atDay(1);
        LocalDate end = LocalDate.of(year, month, lastDay);
        Clock clock = Clock.fixed(start.atTime(12, 0).atZone(ZoneId.of("America/Sao_Paulo")).toInstant(),
                ZoneId.of("America/Sao_Paulo"));
        dashboardService = new DashboardService(cashFlowReportService, competenceReportService,
                budgetService, accountRepository, invoiceRepository, billRepository, currentUserService, clock);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(cashFlowReportService.monthly(year, month)).thenReturn(new MonthlyCashFlowResponse(
                year, month, start, end, new CashFlowSummaryResponse(
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of())));
        when(competenceReportService.generate(start, end)).thenReturn(new CompetenceReportResponse(
                start, end, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of(), List.of()));
        when(invoiceRepository.sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID, start, end, Set.of(InvoiceStatus.OPEN))).thenReturn(money("125.50"));

        var response = dashboardService.get();

        assertThat(response.monthlyOpenInvoices().amount()).isEqualByComparingTo("125.50");
        assertThat(response.periodStart()).isEqualTo(start);
        assertThat(response.periodEnd()).isEqualTo(end);
        verify(invoiceRepository).sumTotalAmountOwnedByUserIdAndStatusIn(USER_ID, Set.of(InvoiceStatus.OPEN));
        verify(invoiceRepository).sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID, start, end, Set.of(InvoiceStatus.OPEN));
        verifyNoMoreInteractions(invoiceRepository);
    }

    private BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }
}
