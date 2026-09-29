package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.report.dto.CashFlowComparisonResponse;
import com.amorim.finance_manager.report.dto.CashFlowSummaryResponse;
import com.amorim.finance_manager.report.dto.CashFlowTotalsResponse;
import com.amorim.finance_manager.report.dto.CategoryCashFlowResponse;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CashFlowAggregate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class CashFlowCalculator {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public CashFlowSummaryResponse summarize(
            List<CashFlowAggregate> rows,
            Map<UUID, String> categoryNames
    ) {
        return summarize(rows, categoryNames, ZERO);
    }

    public CashFlowSummaryResponse summarize(
            List<CashFlowAggregate> rows,
            Map<UUID, String> categoryNames,
            BigDecimal invoiceOutflows
    ) {
        BigDecimal inflows = ZERO;
        BigDecimal directExpenses = ZERO;
        BigDecimal totalInvoiceOutflows = invoiceOutflows;

        Map<UUID, BigDecimal> incomeCategories = new HashMap<>();
        Map<UUID, BigDecimal> expenseCategories = new HashMap<>();

        for (CashFlowAggregate row : rows) {
            switch (row.type()) {
                case INCOME -> {
                    inflows = inflows.add(row.amount());
                    incomeCategories.merge(
                            row.categoryId(),
                            row.amount(),
                            BigDecimal::add
                    );
                }

                case EXPENSE -> {
                    directExpenses= directExpenses.add(row.amount());
                    expenseCategories.merge(
                            row.categoryId(),
                            row.amount(),
                            BigDecimal::add
                    );
                }

                case CREDIT_CARD_PAYMENT -> totalInvoiceOutflows = totalInvoiceOutflows.add(row.amount());

                default -> throw new IllegalArgumentException(
                        "Tipo não elegível para o resumo de caixa: " + row.type()
                );
            }
        }

        BigDecimal outflows = directExpenses.add(totalInvoiceOutflows);

        return new CashFlowSummaryResponse(
                inflows,
                outflows,
                inflows.subtract(outflows),
                totalInvoiceOutflows,
                toCategories(incomeCategories, categoryNames),
                toCategories(expenseCategories, categoryNames)
        );
    }

    public CashFlowComparisonResponse compare(
            CashFlowSummaryResponse current,
            CashFlowSummaryResponse previous) {
        return new CashFlowComparisonResponse(
                current.inflows().subtract(previous.inflows()),
                current.outflows().subtract(previous.outflows()),
                current.net().subtract(previous.net())
        );
    }

    private List<CategoryCashFlowResponse> toCategories(
            Map<UUID, BigDecimal> totals,
            Map<UUID, String> names
    ) {
        return totals.entrySet()
                .stream()
                .map(entry -> new CategoryCashFlowResponse(
                        entry.getKey(),
                        entry.getKey() == null
                                ? "Sem categoria"
                                : names.getOrDefault(
                                entry.getKey(),
                                "Categoria indisponível"
                        ),
                        entry.getValue()
                ))
                .sorted(
                        Comparator
                                .comparing(CategoryCashFlowResponse::amount)
                                .reversed()
                                .thenComparing(CategoryCashFlowResponse::name)
                                .thenComparing(category ->
                                        category.categoryId() == null
                                                ? ""
                                                : category.categoryId().toString()
                                )
                )
                .toList();
    }

    public CashFlowTotalsResponse summarizeTotals(
            List<AnnualCashFlowAggregate> rows,
            BigDecimal invoiceOutflows
    ) {
        BigDecimal inflows = ZERO;
        BigDecimal outflows = ZERO;

        for (AnnualCashFlowAggregate row : rows) {
            switch (row.type()) {
                case INCOME -> inflows = inflows.add(row.amount());

                case EXPENSE, CREDIT_CARD_PAYMENT -> outflows = outflows.add(row.amount());

                default -> throw new IllegalArgumentException(
                        "Tipo não elegível para relatório anual de caixa: " + row.type()
                );
            }
        }
        outflows = outflows.add(invoiceOutflows);

        return new CashFlowTotalsResponse(
                inflows,
                outflows,
                inflows.subtract(outflows)
        );
    }

    public CashFlowTotalsResponse summarizeTotals(List<AnnualCashFlowAggregate> rows) {
        return summarizeTotals(rows, ZERO);
    }
}
