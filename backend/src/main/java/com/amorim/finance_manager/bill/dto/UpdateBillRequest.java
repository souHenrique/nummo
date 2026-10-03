package com.amorim.finance_manager.bill.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateBillRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate dueDate,
        @NotNull UUID categoryId,
        @NotNull @PositiveOrZero Long expectedVersion
) {
    public UpdateBillRequest {
        if (description != null) description = description.trim();
    }
}
