package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.AnnualCashFlowMonthResponse;
import com.amorim.finance_manager.report.dto.AnnualCompetenceReportResponse;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CompetenceAggregate;
import com.amorim.finance_manager.report.repository.CompetenceReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CompetenceReportService {

    private static final Set<TransactionType> INCLUDED_TYPES = Set.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PURCHASE
    );

    private final CompetenceReportRepository reportRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final CompetenceReportCalculator calculator;

    public CompetenceReportResponse generate(LocalDate startDate, LocalDate endDate) {
        validatePeriod(startDate, endDate);

        UUID userId = currentUserService.getCurrentUserId();

        List<CompetenceAggregate> rows = reportRepository.aggregate(
                userId,
                startDate,
                endDate,
                TransactionStatus.COMPLETED,
                INCLUDED_TYPES
        );

        Map<UUID, String> categoryNames = loadCategoryNames(userId, rows);

        return calculator.calculate(
                startDate,
                endDate,
                rows,
                categoryNames
        );
    }

    public BigDecimal creditCardPurchaseOutflows(Integer referenceYear, Integer referenceMonth) {
        UUID userId = currentUserService.getCurrentUserId();

        return reportRepository.sumCreditCardPurchasesByInvoicePeriod(
                userId,
                TransactionStatus.COMPLETED,
                TransactionType.CREDIT_CARD_PURCHASE,
                referenceYear,
                referenceMonth
        );
    }

    public AnnualCompetenceReportResponse annual(Integer year) {
        validateYear(year);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);
        UUID userId = currentUserService.getCurrentUserId();

        Map<Integer, List<AnnualCashFlowAggregate>> rowsByMonth = reportRepository.aggregateByMonth(
                        userId,
                        start,
                        end,
                        TransactionStatus.COMPLETED,
                        INCLUDED_TYPES
                )
                .stream()
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

        return categoryRepository
                .findAllByUserIdAndIdIn(userId, categoryIds)
                .stream()
                .collect(Collectors.toMap(
                        Category::getId,
                        Category::getName
                ));
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new InvalidReportPeriodException("As datas inicial e final são obrigatórias");
        }

        if (startDate.isAfter(endDate)) {
            throw new InvalidReportPeriodException("A data inicial não pode ser posterior à data final");
        }
    }

    private void validateYear(Integer year) {
        if (year == null || year < 1 || year > 9999) {
            throw new InvalidReportPeriodException("O ano deve estar entre 1 e 9999");
        }
    }
}
