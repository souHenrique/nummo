package com.amorim.finance_manager.bill.dto;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateBillRequest(
        @NotBlank @Size(max = 255) String description,
        @Schema(description = "Valor de cada boleto/parcela; não o total do financiamento")
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Schema(description = "Vencimento do primeiro boleto; os seguintes vencem mensalmente")
        @NotNull LocalDate firstDueDate,
        @NotNull @Min(1) @Max(600) Integer installmentCount,
        @NotNull UUID categoryId
) {
    public CreateBillRequest {
        if (description != null) description = description.trim();
    }
}
