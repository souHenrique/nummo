package com.amorim.finance_manager.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(description = "Dados opcionais para atualização do perfil do usuário")
public record UpdateProfileRequest(

        @Schema(description = "Novo nome do usuário", example = "Henrique Amorim Silva")
        @Size(max = 120, message = "Nome deve possuir no máximo 120 caracteres")
        String name,

        @Schema(description = "Novo e-mail do usuário", example = "henrique.silva@example.com", format = "email")
        @Email(message = "E-mail inválido")
        @Size(max = 320, message = "E-mail deve possuir no máximo 320 caracteres")
        String email,

        @Schema(
                description = "Senha atual obrigatória quando o e-mail for alterado",
                example = "SenhaSegura123!",
                format = "password",
                writeOnly = true
        )
        String currentPassword
) {
    public UpdateProfileRequest {
        name = name == null ? null : name.trim();

        email = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        currentPassword = currentPassword == null ? null : currentPassword.trim();
    }
}
