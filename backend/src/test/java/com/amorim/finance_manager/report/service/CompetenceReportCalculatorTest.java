package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.report.dto.CategoryCashFlowResponse;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetenceReportCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 30);
    private static final UUID INCOME_CATEGORY = UUID.randomUUID();
    private static final UUID EXPENSE_CATEGORY = UUID.randomUUID();

    private final CompetenceReportCalculator calculator = new CompetenceReportCalculator();

    @Test
    void shouldReturnZeroTotalsAndEmptyCategoriesWithoutEligibleMovements() {
        var response = calculator.calculate(START, END, List.of(), Map.of());

        assertThat(response.startDate()).isEqualTo(START);
        assertThat(response.endDate()).isEqualTo(END);
        assertThat(response.totalIncome()).isEqualByComparingTo("0.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("0.00");
        assertThat(response.result()).isEqualByComparingTo("0.00");
        assertThat(response.incomeCategories()).isEmpty();
        assertThat(response.expenseCategories()).isEmpty();
    }

    @Test
    void shouldCountCardPurchasesAsExpensesAndGroupValuesByCategory() {
        UUID otherExpenseCategory = UUID.randomUUID();

        var response = calculator.calculate(
                START,
                END,
                List.of(
                        row(TransactionType.INCOME, INCOME_CATEGORY, "1500.00"),
                        row(TransactionType.EXPENSE, EXPENSE_CATEGORY, "200.00"),
                        row(TransactionType.CREDIT_CARD_PURCHASE, EXPENSE_CATEGORY, "300.00"),
                        row(TransactionType.CREDIT_CARD_PURCHASE, otherExpenseCategory, "700.00")
                ),
                Map.of(
                        INCOME_CATEGORY, "Salário",
                        EXPENSE_CATEGORY, "Mercado",
                        otherExpenseCategory, "Viagem"
                )
        );

        assertThat(response.totalIncome()).isEqualByComparingTo("1500.00");
        assertThat(response.totalExpenses()).isEqualByComparingTo("1200.00");
        assertThat(response.result()).isEqualByComparingTo("300.00");
        assertThat(response.incomeCategories()).singleElement()
                .extracting(CategoryCashFlowResponse::amount)
                .isEqualTo(new BigDecimal("1500.00"));
        assertThat(response.expenseCategories())
                .extracting(CategoryCashFlowResponse::categoryId)
                .containsExactly(otherExpenseCategory, EXPENSE_CATEGORY);
        assertThat(response.expenseCategories().get(1).amount()).isEqualByComparingTo("500.00");
    }

    @Test
    void shouldIncludeCreditCardPurchasesInMonthlyCompetenceTotals() {
        var totals = calculator.summarizeTotals(List.of(
                new AnnualCashFlowAggregate(9, TransactionType.INCOME, new BigDecimal("2000.00")),
                new AnnualCashFlowAggregate(9, TransactionType.EXPENSE, new BigDecimal("300.00")),
                new AnnualCashFlowAggregate(
                        9,
                        TransactionType.CREDIT_CARD_PURCHASE,
                        new BigDecimal("700.00")
                )
        ));

        assertThat(totals.inflows()).isEqualByComparingTo("2000.00");
        assertThat(totals.outflows()).isEqualByComparingTo("1000.00");
        assertThat(totals.net()).isEqualByComparingTo("1000.00");
    }

    @Test
    void shouldDistinguishMissingAndUnavailableCategories() {
        var response = calculator.calculate(
                START,
                END,
                List.of(
                        row(TransactionType.EXPENSE, null, "20.00"),
                        row(TransactionType.EXPENSE, EXPENSE_CATEGORY, "10.00")
                ),
                Map.of()
        );

        assertThat(response.expenseCategories()).hasSize(2);
        assertThat(response.expenseCategories().get(0).name()).isEqualTo("Sem categoria");
        assertThat(response.expenseCategories().get(1).name()).isEqualTo("Categoria indisponível");
    }

    @ParameterizedTest
    @EnumSource(
            value = TransactionType.class,
            names = {"CREDIT_CARD_PAYMENT", "TRANSFER", "ADJUSTMENT"}
    )
    void shouldRejectTypesOutsideTheCompetenceContract(TransactionType type) {
        assertThatThrownBy(() -> calculator.calculate(
                START,
                END,
                List.of(row(type, null, "1000.00")),
                Map.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(type.name());
    }

    private CompetenceAggregate row(TransactionType type, UUID categoryId, String amount) {
        return new CompetenceAggregate(type, categoryId, new BigDecimal(amount));
    }
}
