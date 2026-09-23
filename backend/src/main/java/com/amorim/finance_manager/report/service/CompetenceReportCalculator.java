package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.report.dto.CategoryCashFlowResponse;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.CashFlowTotalsResponse;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Component
public class CompetenceReportCalculator {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public CompetenceReportResponse calculate(
            LocalDate startDate,
            LocalDate endDate,
            List<CompetenceAggregate> rows,
            Map<UUID, String> categoryNames
    ) {
        BigDecimal totalIncome = ZERO;
        BigDecimal totalExpenses = ZERO;

        Map<UUID, BigDecimal> incomeCategories = new HashMap<>();
        Map<UUID, BigDecimal> expenseCategories = new HashMap<>();

        for (CompetenceAggregate row : rows) {
            switch (row.type()) {
                case INCOME -> {
                    totalIncome = totalIncome.add(row.amount());
                    incomeCategories.merge(row.categoryId(), row.amount(), BigDecimal::add);
                }

                case EXPENSE, CREDIT_CARD_PURCHASE -> {
                    totalExpenses = totalExpenses.add(row.amount());
                    expenseCategories.merge(row.categoryId(), row.amount(), BigDecimal::add);
                }

                default -> throw new IllegalArgumentException(
                        "Tipo não elegível para relatório de competência: "
                            + row.type()
                );
            }
        }
        return new CompetenceReportResponse(
                startDate,
                endDate,
                totalIncome,
                totalExpenses,
                totalIncome.subtract(totalExpenses),
                toCategories(incomeCategories, categoryNames),
                toCategories(expenseCategories, categoryNames)
        );
    }

    public CashFlowTotalsResponse summarizeTotals(List<AnnualCashFlowAggregate> rows) {
        BigDecimal inflows = ZERO;
        BigDecimal outflows = ZERO;

        for (AnnualCashFlowAggregate row : rows) {
            switch (row.type()) {
                case INCOME -> inflows = inflows.add(row.amount());
                case EXPENSE, CREDIT_CARD_PURCHASE -> outflows = outflows.add(row.amount());
                default -> throw new IllegalArgumentException(
                        "Tipo não elegível para evolução por competência: " + row.type()
                );
            }
        }

        return new CashFlowTotalsResponse(
                inflows,
                outflows,
                inflows.subtract(outflows)
        );
    }

    private List<CategoryCashFlowResponse> toCategories(Map<UUID, BigDecimal> totals, Map<UUID, String> names) {
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
                )
                .toList();
    }
}
