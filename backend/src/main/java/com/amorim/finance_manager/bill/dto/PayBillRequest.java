package com.amorim.finance_manager.bill.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;

public record PayBillRequest(
        @NotNull UUID sourceAccountId,
        @NotNull @PastOrPresent LocalDate paymentDate,
        @NotNull @PositiveOrZero Long expectedVersion
) {}
