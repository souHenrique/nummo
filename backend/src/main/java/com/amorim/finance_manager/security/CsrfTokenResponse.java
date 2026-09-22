package com.amorim.finance_manager.security;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Token CSRF emitido pelo Spring Security para operações que alteram dados")
public record CsrfTokenResponse(
        @Schema(description = "Valor do token CSRF", example = "550e8400-e29b-41d4-a716-446655440000")
        String token,

        @Schema(description = "Cabeçalho HTTP que deve receber o token", example = "X-XSRF-TOKEN")
        String headerName
) {
}
