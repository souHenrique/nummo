package com.amorim.finance_manager.bill.dto;

import com.amorim.finance_manager.bill.entity.Bill;
import com.amorim.finance_manager.bill.entity.BillStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record BillResponse(
        UUID id, String description, BigDecimal amount, LocalDate dueDate,
        UUID categoryId, UUID seriesId, Integer installmentNumber, Integer installmentCount,
        BillStatus status, UUID paymentTransactionId, Long version
) {
    public static BillResponse from(Bill bill) {
        return new BillResponse(bill.getId(), bill.getDescription(), bill.getAmount(),
                bill.getDueDate(), bill.getCategoryId(), bill.getSeriesId(),
                bill.getInstallmentNumber(), bill.getInstallmentCount(), bill.getStatus(),
                bill.getPaymentTransactionId(), bill.getVersion());
    }
}
