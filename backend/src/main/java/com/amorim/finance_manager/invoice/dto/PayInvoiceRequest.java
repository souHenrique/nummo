package com.amorim.finance_manager.invoice.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record PayInvoiceRequest(
        UUID sourceAccountId,

        @NotNull
        LocalDate paymentDate,

        @NotNull
        Long expectedVersion
) {
}
