package com.amorim.finance_manager.report.service;

import com.amorim.finance_manager.category.repository.CategoryRepository;
import com.amorim.finance_manager.invoice.entity.InvoiceStatus;
import com.amorim.finance_manager.invoice.repository.InvoiceRepository;
import com.amorim.finance_manager.report.repository.CashFlowReportRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceOutflowReportServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate END = LocalDate.of(2026, 10, 31);

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
    void shouldUseInvoiceAmountsAtDueDateInsteadOfInvoicePaymentTransactions() {
        when(currentUserService.getCurrentUserId()).thenReturn(USER_ID);
        when(reportRepository.aggregate(
                USER_ID,
                START,
                END,
                TransactionStatus.COMPLETED,
                List.of(TransactionType.INCOME, TransactionType.EXPENSE)
        )).thenReturn(List.of());
        when(invoiceRepository.sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID,
                START,
                END,
                Set.of(InvoiceStatus.OPEN, InvoiceStatus.CLOSED, InvoiceStatus.PAID)
        )).thenReturn(new BigDecimal("700.00"));

        var report = service.monthly(2026, 10);

        assertThat(report.summary().invoiceOutflows()).isEqualByComparingTo("700.00");
        assertThat(report.summary().outflows()).isEqualByComparingTo("700.00");
        assertThat(report.summary().net()).isEqualByComparingTo("-700.00");
        verify(reportRepository).aggregate(
                USER_ID,
                START,
                END,
                TransactionStatus.COMPLETED,
                List.of(TransactionType.INCOME, TransactionType.EXPENSE)
        );
        verify(invoiceRepository).sumTotalAmountOwnedByUserIdAndDueDateBetweenAndStatusIn(
                USER_ID,
                START,
                END,
                Set.of(InvoiceStatus.OPEN, InvoiceStatus.CLOSED, InvoiceStatus.PAID)
        );
    }
}
