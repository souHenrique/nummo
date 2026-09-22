package com.amorim.finance_manager.security;

import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.shared.exception.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        boolean csrfFailure = accessDeniedException instanceof CsrfException;
        ApiErrorCode code = csrfFailure
                ? ApiErrorCode.CSRF_VALIDATION_FAILED
                : ApiErrorCode.ACCESS_DENIED;
        String message = csrfFailure
                ? "Token de segurança ausente ou inválido."
                : "Acesso negado.";

        ApiError error = ApiError.of(
                HttpStatus.FORBIDDEN,
                code,
                message,
                request.getRequestURI()
        );

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
