package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyAnnualCashFlowReportServiceTest {

    private static final UUID USER = UUID.randomUUID();
    private static final List<TransactionType> CASH_TYPES = List.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PAYMENT
    );

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
        service = new CashFlowReportService(
                reportRepository,
                invoiceRepository,
                categoryRepository,
                currentUserService,
                new CashFlowCalculator()
        );
    }

    @Test
    void shouldQueryTheExactLeapYearMonthAndGroupCategories() {
        LocalDate start = LocalDate.of(2024, 2, 1);
        LocalDate end = LocalDate.of(2024, 2, 29);
        UUID incomeCategory = UUID.randomUUID();
        UUID expenseCategory = UUID.randomUUID();

        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregate(
                USER, start, end, TransactionStatus.COMPLETED, CASH_TYPES
        )).thenReturn(List.of(
                cashRow(start, TransactionType.INCOME, incomeCategory, "1000.00"),
                cashRow(end, TransactionType.INCOME, incomeCategory, "250.00"),
                cashRow(end, TransactionType.EXPENSE, expenseCategory, "300.00"),
                cashRow(end, TransactionType.CREDIT_CARD_PAYMENT, null, "200.00")
        ));
        when(categoryRepository.findAllByUserIdAndIdIn(
                USER, Set.of(incomeCategory, expenseCategory)
        )).thenReturn(List.of(
                category(incomeCategory, "Salário"),
                category(expenseCategory, "Mercado")
        ));

        var response = service.monthly(2024, 2);

        assertThat(response.year()).isEqualTo(2024);
        assertThat(response.month()).isEqualTo(2);
        assertThat(response.startDate()).isEqualTo(start);
        assertThat(response.endDate()).isEqualTo(end);
        assertThat(response.summary().inflows()).isEqualByComparingTo("1250.00");
        assertThat(response.summary().outflows()).isEqualByComparingTo("500.00");
        assertThat(response.summary().net()).isEqualByComparingTo("750.00");
        assertThat(response.summary().invoicePayments()).isEqualByComparingTo("200.00");
        assertThat(response.summary().incomeCategories()).singleElement()
                .satisfies(item -> {
                    assertThat(item.categoryId()).isEqualTo(incomeCategory);
                    assertThat(item.amount()).isEqualByComparingTo("1250.00");
                });
        assertThat(response.summary().expenseCategories()).singleElement()
                .satisfies(item -> {
                    assertThat(item.categoryId()).isEqualTo(expenseCategory);
                    assertThat(item.amount()).isEqualByComparingTo("300.00");
                });
        verify(reportRepository).aggregate(
                USER, start, end, TransactionStatus.COMPLETED, CASH_TYPES
        );
        verify(categoryRepository).findAllByUserIdAndIdIn(
                USER, Set.of(incomeCategory, expenseCategory)
        );
        verifyNoMoreInteractions(reportRepository, categoryRepository);
    }

    @Test
    void shouldReturnTwelveOrderedMonthsAndZeroFillMissingAnnualMonths() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(currentUserService.getCurrentUserId()).thenReturn(USER);
        when(reportRepository.aggregateByMonth(
                USER, start, end, TransactionStatus.COMPLETED, CASH_TYPES
        )).thenReturn(List.of(
                annualRow(1, TransactionType.INCOME, "1000.00"),
                annualRow(1, TransactionType.EXPENSE, "300.00"),
                annualRow(2, TransactionType.CREDIT_CARD_PAYMENT, "200.00"),
                annualRow(12, TransactionType.INCOME, "50.00")
        ));

        var response = service.annual(2026);

        assertThat(response.year()).isEqualTo(2026);
        assertThat(response.startDate()).isEqualTo(start);
        assertThat(response.endDate()).isEqualTo(end);
        assertThat(response.evolution()).hasSize(12);
        assertThat(response.evolution()).extracting(item -> item.month())
                .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        assertThat(response.evolution().get(0).totals().inflows()).isEqualByComparingTo("1000.00");
        assertThat(response.evolution().get(0).totals().outflows()).isEqualByComparingTo("300.00");
        assertThat(response.evolution().get(0).totals().net()).isEqualByComparingTo("700.00");
        assertThat(response.evolution().get(1).totals().inflows()).isZero();
        assertThat(response.evolution().get(1).totals().outflows()).isEqualByComparingTo("200.00");
        assertThat(response.evolution().get(1).totals().net()).isEqualByComparingTo("-200.00");
        assertThat(response.evolution().get(2).totals().inflows()).isZero();
        assertThat(response.evolution().get(2).totals().outflows()).isZero();
        assertThat(response.evolution().get(2).totals().net()).isZero();
        assertThat(response.evolution().get(11).totals().net()).isEqualByComparingTo("50.00");
        verify(reportRepository).aggregateByMonth(
                USER, start, end, TransactionStatus.COMPLETED, CASH_TYPES
        );
        verifyNoInteractions(categoryRepository);
        verifyNoMoreInteractions(reportRepository);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "null,1",
            "0,1",
            "10000,1",
            "2026,null",
            "2026,0",
            "2026,13"
    }, nullValues = "null")
    void shouldRejectInvalidMonthlyPeriodsBeforeReadingAuthenticationOrFinancialData(
            Integer year,
            Integer month
    ) {
        assertThatThrownBy(() -> service.monthly(year, month))
                .isInstanceOf(InvalidReportPeriodException.class);

        verifyNoInteractions(currentUserService, reportRepository, categoryRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, 10000})
    void shouldRejectInvalidAnnualYearsBeforeReadingAuthenticationOrFinancialData(Integer year) {
        assertThatThrownBy(() -> service.annual(year))
                .isInstanceOf(InvalidReportPeriodException.class);

        verifyNoInteractions(currentUserService, reportRepository, categoryRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldNotReadMonthlyOrAnnualFinancialDataWithoutAuthentication(boolean annual) {
        when(currentUserService.getCurrentUserId())
                .thenThrow(new UnauthenticatedUserException());

        assertThatThrownBy(() -> {
            if (annual) {
                service.annual(2026);
            } else {
                service.monthly(2026, 9);
            }
        }).isInstanceOf(UnauthenticatedUserException.class);

        verifyNoInteractions(reportRepository, categoryRepository);
    }

    private CashFlowAggregate cashRow(
            LocalDate date,
            TransactionType type,
            UUID categoryId,
            String amount
    ) {
        return new CashFlowAggregate(
                date,
                type,
                categoryId,
                new BigDecimal(amount)
        );
    }

    private AnnualCashFlowAggregate annualRow(
            int month,
            TransactionType type,
            String amount
    ) {
        return new AnnualCashFlowAggregate(
                month,
                type,
                new BigDecimal(amount)
        );
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(USER);
        category.setName(name);
        return category;
    }
}
