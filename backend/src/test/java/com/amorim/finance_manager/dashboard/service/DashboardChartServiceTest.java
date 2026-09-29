package com.amorim.finance_manager.dashboard.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.dashboard.repository.DashboardChartRepository;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.service.CompetenceReportCalculator;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardChartServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INCOME_CATEGORY_ID = UUID.randomUUID();
    private static final UUID EXPENSE_CATEGORY_ID = UUID.randomUUID();
    private static final Set<TransactionType> DIRECT_TYPES = Set.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE
    );

    @Mock
    private DashboardChartRepository chartRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CurrentUserService currentUserService;

    private DashboardChartService service;

    @BeforeEach
    void setUp() {
        service = new DashboardChartService(
                chartRepository,
                categoryRepository,
                currentUserService,
                new CompetenceReportCalculator()
        );
    }

    @Test
    void shouldShowCreditCardPurchasesOnlyInTheMonthOfTheInvoiceDueDate() {
        LocalDate start = LocalDate.of(2026, 10, 1);
        LocalDate end = LocalDate.of(2026, 10, 31);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(chartRepository.aggregateDirectTransactions(
                USER_ID, start, end, TransactionStatus.COMPLETED, DIRECT_TYPES
        )).thenReturn(List.of(
                row(TransactionType.INCOME, INCOME_CATEGORY_ID, "4000.00"),
                row(TransactionType.EXPENSE, EXPENSE_CATEGORY_ID, "300.00")
        ));
        when(chartRepository.aggregateCreditCardPurchasesByDueDate(
                USER_ID, start, end, TransactionStatus.COMPLETED, TransactionType.CREDIT_CARD_PURCHASE
        )).thenReturn(List.of(row(TransactionType.CREDIT_CARD_PURCHASE, EXPENSE_CATEGORY_ID, "700.00")));
        when(categoryRepository.findAllByUserIdAndIdIn(
                USER_ID, Set.of(INCOME_CATEGORY_ID, EXPENSE_CATEGORY_ID)
        )).thenReturn(List.of(
                category(INCOME_CATEGORY_ID, "Salário"),
                category(EXPENSE_CATEGORY_ID, "Tecnologia")
        ));

        var response = service.monthly(2026, 10);

        assertThat(response.totalIncome()).isEqualByComparingTo("4000.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("1000.00");
        assertThat(response.expenseCategories()).singleElement().satisfies(category -> {
            assertThat(category.name()).isEqualTo("Tecnologia");
            assertThat(category.amount()).isEqualByComparingTo("1000.00");
        });
        verify(chartRepository).aggregateDirectTransactions(
                USER_ID, start, end, TransactionStatus.COMPLETED, DIRECT_TYPES
        );
        verify(chartRepository).aggregateCreditCardPurchasesByDueDate(
                USER_ID, start, end, TransactionStatus.COMPLETED, TransactionType.CREDIT_CARD_PURCHASE
        );
    }

    @Test
    void shouldPlaceAnInstallmentInItsInvoiceDueMonthInTheAnnualChart() {
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(chartRepository.aggregateDirectTransactionsByMonth(
                USER_ID, start, end, TransactionStatus.COMPLETED, DIRECT_TYPES
        )).thenReturn(List.of(
                annualRow(9, TransactionType.INCOME, "4000.00"),
                annualRow(9, TransactionType.EXPENSE, "300.00")
        ));
        when(chartRepository.aggregateCreditCardPurchasesByDueMonth(
                USER_ID, start, end, TransactionStatus.COMPLETED, TransactionType.CREDIT_CARD_PURCHASE
        )).thenReturn(List.of(annualRow(10, TransactionType.CREDIT_CARD_PURCHASE, "700.00")));

        var response = service.annual(2026);

        assertThat(response.evolution()).hasSize(12);
        assertThat(response.evolution().get(8).totals().inflows()).isEqualByComparingTo("4000.00");
        assertThat(response.evolution().get(8).totals().outflows()).isEqualByComparingTo("300.00");
        assertThat(response.evolution().get(9).totals().inflows()).isZero();
        assertThat(response.evolution().get(9).totals().outflows()).isEqualByComparingTo("700.00");
        verify(chartRepository).aggregateDirectTransactionsByMonth(
                USER_ID, start, end, TransactionStatus.COMPLETED, DIRECT_TYPES
        );
        verify(chartRepository).aggregateCreditCardPurchasesByDueMonth(
                USER_ID, start, end, TransactionStatus.COMPLETED, TransactionType.CREDIT_CARD_PURCHASE
        );
        verifyNoMoreInteractions(chartRepository, categoryRepository);
    }

    private CompetenceAggregate row(TransactionType type, UUID categoryId, String amount) {
        return new CompetenceAggregate(type, categoryId, new BigDecimal(amount));
    }

    private AnnualCashFlowAggregate annualRow(int month, TransactionType type, String amount) {
        return new AnnualCashFlowAggregate(month, type, new BigDecimal(amount));
    }

    private Category category(UUID id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setUserId(USER_ID);
        category.setName(name);
        return category;
    }
}
