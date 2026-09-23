package com.amorim.finance_manager.creditcard.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateCreditCardPurchaseRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull LocalDate purchaseDate,
        @NotNull UUID categoryId,
        @NotNull @Positive Integer installmentCount
) {
    public UpdateCreditCardPurchaseRequest {
        description = description == null ? null : description.trim();
    }
}
