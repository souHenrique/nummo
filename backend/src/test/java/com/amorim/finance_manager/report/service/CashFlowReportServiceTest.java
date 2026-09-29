package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.projection.CashFlowAggregate;
import com.amorim.finance_manager.report.repository.CashFlowReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.shared.exception.UnauthenticatedUserException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashFlowReportServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final LocalDate DATE = LocalDate.of(2026, 9, 3);
    private static final List<TransactionType> CASH_TYPES = List.of(
            TransactionType.INCOME, TransactionType.EXPENSE, TransactionType.CREDIT_CARD_PAYMENT);
    private static final List<TransactionType> CASH_OUTFLOW_TYPES = List.of(
            TransactionType.EXPENSE, TransactionType.CREDIT_CARD_PAYMENT);

    @Mock
    private CashFlowReportRepository reportRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrentUserService currentUserService;

    private CashFlowReportService service;

    @BeforeEach
    void setUp() {
        service = new CashFlowReportService(reportRepository, invoiceRepository, categoryRepository,
                currentUserService, new CashFlowCalculator());
    }

    @Test
    void shouldQueryTheExactDayCompletedStatusAndCashTypesForTheAuthenticatedUser() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, DATE, DATE, TransactionStatus.COMPLETED, CASH_TYPES))
                .thenReturn(List.of(row(DATE, TransactionType.INCOME, null, "125.10")));

        var response = service.daily(DATE);

        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.summary().inflows()).isEqualByComparingTo("125.10");
        assertThat(response.summary().net()).isEqualByComparingTo("125.10");
        verify(reportRepository).aggregate(USER, DATE, DATE, TransactionStatus.COMPLETED, CASH_TYPES);
        verifyNoMoreInteractions(reportRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldSumAllCompletedCashOutflowsWithoutCreditCardPurchases() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.sumAmountsByUserIdAndTypes(
                USER,
                TransactionStatus.COMPLETED,
                CASH_OUTFLOW_TYPES
        )).thenReturn(new BigDecimal("9800.00"));

        assertThat(service.totalOutflows()).isEqualByComparingTo("9800.00");
        verify(reportRepository).sumAmountsByUserIdAndTypes(
                USER,
                TransactionStatus.COMPLETED,
                CASH_OUTFLOW_TYPES
        );
        verifyNoInteractions(categoryRepository);
        verifyNoMoreInteractions(reportRepository);
    }

    @ParameterizedTest
    @CsvSource({
            "2026-09-03,2026-08-31,2026-09-06,2026-08-24,2026-08-30",
            "2026-08-31,2026-08-31,2026-09-06,2026-08-24,2026-08-30",
            "2026-09-06,2026-08-31,2026-09-06,2026-08-24,2026-08-30",
            "2026-09-07,2026-09-07,2026-09-13,2026-08-31,2026-09-06",
            "2026-01-01,2025-12-29,2026-01-04,2025-12-22,2025-12-28",
            "2024-02-29,2024-02-26,2024-03-03,2024-02-19,2024-02-25"
    })
    void shouldNormalizeAnyReferenceDayToMondaySundayAndQueryBothWeeksOnce(
            LocalDate date, LocalDate start, LocalDate end, LocalDate previousStart, LocalDate previousEnd) {
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, previousStart, end, TransactionStatus.COMPLETED, CASH_TYPES))
                .thenReturn(List.of());

        var response = service.weekly(date);

        assertThat(response.currentWeek().startDate()).isEqualTo(start);
        assertThat(response.currentWeek().endDate()).isEqualTo(end);
        assertThat(response.previousWeek().startDate()).isEqualTo(previousStart);
        assertThat(response.previousWeek().endDate()).isEqualTo(previousEnd);
        assertThat(response.currentWeek().summary().net()).isZero();
        assertThat(response.previousWeek().summary().net()).isZero();
        assertThat(response.comparison().netDifference()).isZero();
        verify(reportRepository).aggregate(USER, previousStart, end, TransactionStatus.COMPLETED, CASH_TYPES);
        verifyNoMoreInteractions(reportRepository);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldSeparateTheTwoWeeksAndResolveDistinctCategoryNamesInOneUserScopedBatch() {
        UUID income = UUID.randomUUID();
        UUID expense = UUID.randomUUID();
        UUID invoiceCategory = UUID.randomUUID();
        LocalDate start = LocalDate.of(2026, 8, 31);
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, start.minusWeeks(1), start.plusDays(6),
                TransactionStatus.COMPLETED, CASH_TYPES)).thenReturn(List.of(
                row(start.minusWeeks(1), TransactionType.INCOME, income, "400.00"),
                row(start.minusDays(1), TransactionType.EXPENSE, expense, "100.00"),
                row(start, TransactionType.INCOME, income, "600.00"),
                row(start.plusDays(6), TransactionType.EXPENSE, expense, "50.00"),
                row(start.plusDays(1), TransactionType.CREDIT_CARD_PAYMENT, invoiceCategory, "200.00")
        ));
        when(categoryRepository.findAllByUserIdAndIdIn(USER, Set.of(income, expense)))
                .thenReturn(List.of(category(income, "Income"), category(expense, "Expense")));

        var response = service.weekly(DATE);

        assertThat(response.currentWeek().summary().inflows()).isEqualByComparingTo("600.00");
        assertThat(response.currentWeek().summary().outflows()).isEqualByComparingTo("250.00");
        assertThat(response.currentWeek().summary().invoicePayments()).isEqualByComparingTo("200.00");
        assertThat(response.previousWeek().summary().inflows()).isEqualByComparingTo("400.00");
        assertThat(response.previousWeek().summary().outflows()).isEqualByComparingTo("100.00");
        assertThat(response.comparison().inflowsDifference()).isEqualByComparingTo("200.00");
        assertThat(response.comparison().outflowsDifference()).isEqualByComparingTo("150.00");
        assertThat(response.comparison().netDifference()).isEqualByComparingTo("50.00");
        assertThat(response.currentWeek().summary().incomeCategories().getFirst().name()).isEqualTo("Income");
        verify(categoryRepository).findAllByUserIdAndIdIn(USER, Set.of(income, expense));
        verifyNoMoreInteractions(categoryRepository);
        verify(reportRepository).aggregate(USER, start.minusWeeks(1), start.plusDays(6),
                TransactionStatus.COMPLETED, CASH_TYPES);
        verifyNoMoreInteractions(reportRepository);
    }

    @Test
    void shouldNotQueryCategoryNamesForInvoicePaymentsEvenIfTheyCarryACategoryId() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, DATE, DATE, TransactionStatus.COMPLETED, CASH_TYPES))
                .thenReturn(List.of(row(DATE, TransactionType.CREDIT_CARD_PAYMENT, UUID.randomUUID(), "80.00")));

        assertThat(service.daily(DATE).summary().expenseCategories()).isEmpty();
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldUseUnavailableNameWhenTheUserScopedCategoryLookupDoesNotFindAnId() {
        UUID category = UUID.randomUUID();
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, DATE, DATE, TransactionStatus.COMPLETED, CASH_TYPES))
                .thenReturn(List.of(row(DATE, TransactionType.EXPENSE, category, "30.00")));
        when(categoryRepository.findAllByUserIdAndIdIn(USER, Set.of(category))).thenReturn(List.of());

        assertThat(service.daily(DATE).summary().expenseCategories().getFirst().name())
                .isEqualTo("Categoria indisponível");
        verify(categoryRepository).findAllByUserIdAndIdIn(USER, Set.of(category));
        verifyNoMoreInteractions(categoryRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0000-12-31", "+10000-01-01", "-999999999-01-01", "+999999999-12-31"})
    void shouldRejectInvalidDatesBeforeQueryingEitherReport(String value) {
        LocalDate date = value == null ? null : LocalDate.parse(value);

        assertThatThrownBy(() -> service.daily(date)).isInstanceOf(InvalidReportPeriodException.class);
        assertThatThrownBy(() -> service.weekly(date)).isInstanceOf(InvalidReportPeriodException.class);
        verifyNoInteractions(reportRepository, categoryRepository, currentUserService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0001-01-01", "9999-12-31"})
    void shouldAcceptDailyDateLimitsButRejectWeeklyIntervalsThatCrossThem(LocalDate date) {
        assertThatThrownBy(() -> service.weekly(date)).isInstanceOf(InvalidReportPeriodException.class);
        verifyNoInteractions(reportRepository, categoryRepository, currentUserService);

        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(USER, date, date, TransactionStatus.COMPLETED, CASH_TYPES))
                .thenReturn(List.of());

        assertThat(service.daily(date).date()).isEqualTo(date);
        verify(reportRepository).aggregate(USER, date, date, TransactionStatus.COMPLETED, CASH_TYPES);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldNotQueryFinancialDataWithoutAnAuthenticatedUser(boolean weekly) {
        when(currentUserService.getCurrentUserId()).thenThrow(new UnauthenticatedUserException());

        assertThatThrownBy(() -> {
            if (weekly) service.weekly(DATE);
            else service.daily(DATE);
        }).isInstanceOf(UnauthenticatedUserException.class);
        verifyNoInteractions(reportRepository, categoryRepository);
    }

    @Test
    void shouldPropagateRepositoryFailureInsteadOfReturningAFalseZeroReport() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        IllegalStateException failure = new IllegalStateException("Database unavailable");
        when(reportRepository.aggregate(eq(USER), any(), any(), eq(TransactionStatus.COMPLETED), anyCollection()))
                .thenThrow(failure);

        assertThatThrownBy(() -> service.daily(DATE)).isSameAs(failure);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void shouldDeclareAReadOnlyRepeatableReadTransactionForTheReportSnapshot() {
        Transactional transaction = CashFlowReportService.class.getAnnotation(Transactional.class);

        assertThat(transaction).isNotNull();
        assertThat(transaction.readOnly()).isTrue();
        assertThat(transaction.isolation()).isEqualTo(Isolation.REPEATABLE_READ);
    }

    private CashFlowAggregate row(LocalDate date, TransactionType type, UUID category, String amount) {
        return new CashFlowAggregate(date, type, category, new BigDecimal(amount));
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(USER);
        category.setName(name);
        return category;
    }
}
