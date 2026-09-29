package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.entity.Category;
import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.dto.*;
import com.amorim.finance_manager.report.projection.AnnualCashFlowAggregate;
import com.amorim.finance_manager.report.projection.CashFlowAggregate;
import com.amorim.finance_manager.report.projection.InvoiceDueMonthAggregate;
import com.amorim.finance_manager.report.repository.CashFlowReportRepository;
import com.amorim.finance_manager.shared.exception.InvalidReportPeriodException;
import com.amorim.finance_manager.transaction.entity.TransactionStatus;
import com.amorim.finance_manager.transaction.entity.TransactionType;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class CashFlowReportService {

    private static final LocalDate MIN_DATE = LocalDate.of(1, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private static final List<TransactionType> CASH_TYPES = List.of(
            TransactionType.INCOME,
            TransactionType.EXPENSE
    );

    private static final List<TransactionType> CASH_OUTFLOW_TYPES = List.of(
            TransactionType.EXPENSE,
            TransactionType.CREDIT_CARD_PAYMENT
    );

    private final CashFlowReportRepository reportRepository;
    private final InvoiceRepository invoiceRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserService currentUserService;
    private final CashFlowCalculator calculator;

    public DailyCashFlowResponse daily(LocalDate date) {
        validatePeriod(date, date);

        UUID userId = currentUserService.getCurrentUserId();

        List<CashFlowAggregate> rows = aggregate(userId, date, date);
        Map<UUID, String> names = loadCategoryNames(userId, rows);
        BigDecimal invoiceOutflows = invoiceOutflows(userId, date, date);

        return new DailyCashFlowResponse(
                date,
                calculator.summarize(rows, names, invoiceOutflows)
        );
    }

    public WeeklyCashFlowResponse weekly(LocalDate date) {
        validatePeriod(date, date);

        LocalDate start = date.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate end = start.plusDays(6);

        LocalDate previousStart = start.minusWeeks(1);
        LocalDate previousEnd = start.minusDays(1);

        validatePeriod(previousStart, end);

        UUID userId = currentUserService.getCurrentUserId();

        List<CashFlowAggregate> rows = aggregate(
                userId,
                previousStart,
                end
        );

        Map<UUID, String> names = loadCategoryNames(userId, rows);

        List<CashFlowAggregate> currentRows = rows.stream()
                .filter(row -> !row.effectiveDate().isBefore(start))
                .toList();

        List<CashFlowAggregate> previousRows = rows.stream()
                .filter(row -> row.effectiveDate().isBefore(start))
                .toList();

        CashFlowSummaryResponse current = calculator.summarize(
                currentRows, names, invoiceOutflows(userId, start, end)
        );

        CashFlowSummaryResponse previous = calculator.summarize(
                previousRows, names, invoiceOutflows(userId, previousStart, previousEnd)
        );

        return new WeeklyCashFlowResponse(
                new CashFlowPeriodResponse(start, end, current),
                new CashFlowPeriodResponse(
                        previousStart,
                        previousEnd,
                        previous
                ),
                calculator.compare(current, previous)
        );
    }

    public MonthlyCashFlowResponse monthly(Integer year, Integer month) {
        validateYearMonth(year, month);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());

        UUID userId = currentUserService.getCurrentUserId();

        List<CashFlowAggregate> rows = aggregate(userId, start, end);
        Map<UUID, String> categoryNames = loadCategoryNames(userId, rows);
        BigDecimal invoiceOutflows = invoiceOutflows(userId, start, end);

        return new MonthlyCashFlowResponse(
                year,
                month,
                start,
                end,
                calculator.summarize(rows, categoryNames, invoiceOutflows)
        );
    }

    public AnnualCashFlowResponse annual(Integer year) {
        validateYear(year);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        UUID userId = currentUserService.getCurrentUserId();

        List<AnnualCashFlowAggregate> rows =
                reportRepository.aggregateByMonth(
                        userId,
                        start,
                        end,
                        TransactionStatus.COMPLETED,
                        CASH_TYPES
                );

        Map<Integer, List<AnnualCashFlowAggregate>> rowsByMonth =
                rows.stream()
                        .collect(Collectors.groupingBy(
                                AnnualCashFlowAggregate::month
                        ));
        Map<Integer, BigDecimal> invoiceOutflowsByMonth = invoiceOutflowsByMonth(userId, start, end);

        List<AnnualCashFlowMonthResponse> evolution =
                IntStream.rangeClosed(1, 12)
                        .mapToObj(month -> new AnnualCashFlowMonthResponse(
                                month,
                                calculator.summarizeTotals(
                                        rowsByMonth.getOrDefault(month, List.of()),
                                        invoiceOutflowsByMonth.getOrDefault(month, BigDecimal.ZERO)
                                )
                        ))
                        .toList();

        return new AnnualCashFlowResponse(
                year,
                start,
                end,
                evolution
        );
    }

    public BigDecimal totalOutflows() {
        UUID userId = currentUserService.getCurrentUserId();

        return reportRepository.sumAmountsByUserIdAndTypes(
                userId,
                TransactionStatus.COMPLETED,
                CASH_OUTFLOW_TYPES
        );
    }

    private List<CashFlowAggregate> aggregate(
            UUID userId,
            LocalDate start,
            LocalDate end
    ) {
        return reportRepository.aggregate(
                userId,
                start,
                end,
                TransactionStatus.COMPLETED,
                CASH_TYPES
        );
    }

    private Map<UUID, String> loadCategoryNames(
            UUID userId,
            List<CashFlowAggregate> rows
    ) {
        Set<UUID> categoryIds = rows.stream()
                .map(CashFlowAggregate::categoryId)
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

    private void validatePeriod(LocalDate start, LocalDate end) {
        if (start == null
                || end == null
                || start.isBefore(MIN_DATE)
                || end.isAfter(MAX_DATE)
                || start.isAfter(end)) {

            throw new InvalidReportPeriodException(
                    "O período deve estar entre 0001-01-01 e 9999-12-31"
            );
        }
    }

    private BigDecimal invoiceOutflows(UUID userId, LocalDate start, LocalDate end) {
        return invoiceRepository.sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                userId,
                start,
                end,
                Set.of(InvoiceStatus.OPEN, InvoiceStatus.CLOSED, InvoiceStatus.PAID)
        );
    }

    private Map<Integer, BigDecimal> invoiceOutflowsByMonth(
            UUID userId,
            LocalDate start,
            LocalDate end
    ) {
        return invoiceRepository.sumTotalAmountByDueMonthOwnedByUserIdAndStatusIn(
                        userId,
                        start,
                        end,
                        Set.of(InvoiceStatus.OPEN, InvoiceStatus.CLOSED, InvoiceStatus.PAID)
                )
                .stream()
                .collect(Collectors.toMap(
                        InvoiceDueMonthAggregate::month,
                        InvoiceDueMonthAggregate::amount
                ));
    }

    private void validateYear(Integer year) {
        if (year == null || year < 1 || year > 9999) {
            throw new InvalidReportPeriodException("O ano deve estar entre 1 e 9999");
        }
    }

    private void validateYearMonth(Integer year, Integer month) {
        validateYear(year);

        if (month == null || month < 1 || month > 12) {
            throw new InvalidReportPeriodException("O mês deve estar entre 1 e 12");
        }
    }
}
