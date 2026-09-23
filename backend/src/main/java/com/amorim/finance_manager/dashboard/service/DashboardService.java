package com.amorim.finance_manager.dashboard.service;

import com.amorim.finance_manager.budget.dto.BudgetResponse;
import com.amorim.finance_manager.budget.service.BudgetService;
import com.amorim.finance_manager.account.repository.AccountRepository;
import com.amorim.finance_manager.dashboard.dto.*;
import com.amorim.finance_manager.dashboard.model.AccountingBasis;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.dto.CompetenceReportResponse;
import com.amorim.finance_manager.report.dto.CashFlowSummaryResponse;
import com.amorim.finance_manager.report.dto.MonthlyCashFlowResponse;
import com.amorim.finance_manager.report.service.CashFlowReportService;
import com.amorim.finance_manager.report.service.CompetenceReportService;
import com.amorim.finance_manager.user.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DashboardService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final CashFlowReportService cashFlowReportService;
    private final CompetenceReportService competenceReportService;
    private final BudgetService budgetService;
    private final AccountRepository accountRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;
    private final Clock financeClock;

    public DashboardResponse get() {
        LocalDate referenceDate = LocalDate.now(financeClock);
        YearMonth period = YearMonth.from(referenceDate);

        LocalDate periodStart = period.atDay(1);
        LocalDate periodEnd = period.atEndOfMonth();

        UUID userId = currentUserService.getCurrentUserId();

        MonthlyCashFlowResponse cash = cashFlowReportService.monthly(period.getYear(), period.getMonthValue());
        CashFlowSummaryResponse monthlyCash = cash.summary();
        BigDecimal totalOutflows = cashFlowReportService.totalOutflows();

        CompetenceReportResponse competence = competenceReportService.generate(periodStart, periodEnd);
        BigDecimal creditCardPurchaseOutflows = competenceReportService
                .creditCardPurchaseOutflows(period.getYear(), period.getMonthValue());

        BigDecimal monthlyBalance = monthlyCash.net();

        BigDecimal consolidatedBalance = accountRepository.sumCurrentBalanceByUserId(userId);

        BigDecimal openInvoices =
                invoiceRepository
                        .sumTotalAmountOwnedByUserIdAndStatusIn(userId, Set.of(InvoiceStatus.OPEN));

        BigDecimal monthlyOpenInvoices = invoiceRepository
                .sumTotalAmountOwnedByUserIdAndReferencePeriodAndStatusIn(
                        userId,
                        period.getMonthValue(),
                        period.getYear(),
                        Set.of(InvoiceStatus.OPEN)
                );

        List<BudgetResponse> budgets = budgetService.findByPeriod(period.getYear(), period.getMonthValue());

        return new DashboardResponse(
                referenceDate,
                period.getYear(),
                period.getMonthValue(),
                periodStart,
                periodEnd,
                cash(monthlyBalance),
                cash(monthlyCash.inflows()),
                cash(totalOutflows),
                cash(monthlyCash.outflows()),
                competence(creditCardPurchaseOutflows),
                competence(competence.totalExpenses()),
                competence(openInvoices),
                competence(monthlyOpenInvoices),
                budgetSummary(budgets),
                cash(consolidatedBalance)
        );
    }

    private DashboardIndicatorResponse cash(BigDecimal amount) {
        return new DashboardIndicatorResponse(AccountingBasis.CASH, amount);
    }

    private DashboardIndicatorResponse competence(BigDecimal amount) {
        return new DashboardIndicatorResponse(AccountingBasis.COMPETENCE, amount);
    }

    private DashboardBudgetResponse budgetSummary(List<BudgetResponse> budgets) {
        BigDecimal totalLimit = budgets.stream()
                .map(BudgetResponse::amountLimit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpent = budgets.stream()
                .map(BudgetResponse::spentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal usagePercentage =
                totalLimit.signum() == 0
                        ? BigDecimal.ZERO.setScale(2)
                        : totalSpent
                                .multiply(ONE_HUNDRED)
                                .divide(totalLimit, 2, RoundingMode.HALF_EVEN);

        List<DashboardBudgetItemResponse> items = budgets.stream()
                .map(budget ->
                        new DashboardBudgetItemResponse(
                                budget.id(),
                                budget.categoryId(),
                                budget.amountLimit(),
                                budget.spentAmount(),
                                budget.usagePercentage(),
                                budget.alertStatus()
                        )
                )
                .toList();

        return new DashboardBudgetResponse(
                AccountingBasis.COMPETENCE,
                totalLimit,
                totalSpent,
                usagePercentage,
                items
        );
    }
}
