package com.amorim.finance_manager.dashboard.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.dashboard.repository.DashboardChartRepository;
import com.amorim.finance_manager.report.dto.AnnualCashFlowMonthResponse;
import com.amorim.finance_manager.report.dto.AnnualCompetenceReportResponse;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.service.CompetenceReportCalculator;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Supplies the dashboard-only charts. Direct entries and expenses follow their
 * effective date; credit-card purchases follow the due date of their invoice.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DashboardChartService {

    private static final Set<TransactionType> DIRECT_TYPES = Set.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE
    );

    private final DashboardChartRepository chartRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final CompetenceReportCalculator calculator;

    public CompetenceReportResponse monthly(Integer year, Integer month) {
        validateYearMonth(year, month);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        UUID userId = currentUserService.getCurrentUserId();

        List<CompetenceAggregate> rows = new java.util.ArrayList<>(
                chartRepository.aggregateDirectTransactions(
                        userId,
                        start,
                        end,
                        TransactionStatus.COMPLETED,
                        DIRECT_TYPES
                )
        );
        rows.addAll(chartRepository.aggregateCreditCardPurchasesByDueDate(
                userId,
                start,
                end,
                TransactionStatus.COMPLETED,
                TransactionType.CREDIT_CARD_PURCHASE
        ));

        return calculator.calculate(start, end, rows, loadCategoryNames(userId, rows));
    }

    public AnnualCompetenceReportResponse annual(Integer year) {
        validateYear(year);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        UUID userId = currentUserService.getCurrentUserId();

        List<AnnualCashFlowAggregate> rows = new java.util.ArrayList<>(
                chartRepository.aggregateDirectTransactionsByMonth(
                        userId,
                        start,
                        end,
                        TransactionStatus.COMPLETED,
                        DIRECT_TYPES
                )
        );
        rows.addAll(chartRepository.aggregateCreditCardPurchasesByDueMonth(
                userId,
                start,
                end,
                TransactionStatus.COMPLETED,
                TransactionType.CREDIT_CARD_PURCHASE
        ));

        Map<Integer, List<AnnualCashFlowAggregate>> rowsByMonth = rows.stream()
                .collect(Collectors.groupingBy(AnnualCashFlowAggregate::month));

        List<AnnualCashFlowMonthResponse> evolution = IntStream.rangeClosed(1, 12)
                .mapToObj(month -> new AnnualCashFlowMonthResponse(
                        month,
                        calculator.summarizeTotals(rowsByMonth.getOrDefault(month, List.of()))
                ))
                .toList();

        return new AnnualCompetenceReportResponse(year, start, end, evolution);
    }

    private Map<UUID, String> loadCategoryNames(UUID userId, List<CompetenceAggregate> rows) {
        Set<UUID> categoryIds = rows.stream()
                .map(CompetenceAggregate::categoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (categoryIds.isEmpty()) {
            return Map.of();
        }

        return categoryRepository.findAllByUserIdAndIdIn(userId, categoryIds)
                .stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }

    private void validateYearMonth(Integer year, Integer month) {
        validateYear(year);
        if (month == null || month < 1 || month > 12) {
            throw new InvalidReportPeriodException("O mês deve estar entre 1 e 12");
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 1 || year > 9999) {
            throw new InvalidReportPeriodException("O ano deve estar entre 1 e 9999");
        }
    }
}
