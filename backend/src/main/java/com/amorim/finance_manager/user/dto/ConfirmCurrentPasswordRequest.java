package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Confirmação da senha atual para uma operação sensível")
public record ConfirmCurrentPasswordRequest(

        @Schema(
                description = "Senha atual do usuário",
                example = "SenhaSegura123!",
                format = "password",
                writeOnly = true
        )
        @NotBlank(message = "Senha atual é obrigatória")
        String currentPassword
) {
}
