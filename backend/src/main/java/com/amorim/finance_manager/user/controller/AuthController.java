package com.amorim.finance_manager.user.controller;

import com.amorim.finance_manager.security.AuthCookieService;
import com.amorim.finance_manager.security.CsrfTokenResponse;
import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.user.api.AuthApiDocs;
import com.amorim.finance_manager.user.dto.AuthResponse;
import com.amorim.finance_manager.user.dto.LoginRequest;
import com.amorim.finance_manager.user.dto.RegisterRequest;
import com.amorim.finance_manager.user.dto.UserResponse;
import com.amorim.finance_manager.user.service.AuthService;
import com.amorim.finance_manager.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.amorim.finance_manager.config.openapi.OpenApiExamples.CSRF_TOKEN_RESPONSE;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Cadastro e autenticação de usuários")
public class AuthController implements AuthApiDocs {

    private final UserService userService;
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    @GetMapping("/csrf")
    @Operation(
            summary = "Obter token CSRF",
            description = "Emite o token exigido nas operações autenticadas que alteram dados"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Token CSRF emitido",
                    content = @Content(
                            schema = @Schema(implementation = CsrfTokenResponse.class),
                            examples = @ExampleObject(value = CSRF_TOKEN_RESPONSE)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Erro interno",
                    content = @Content(schema = @Schema(implementation = ApiError.class))
            )
    })
    public ResponseEntity<CsrfTokenResponse> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new CsrfTokenResponse(csrfToken.getToken(), csrfToken.getHeaderName()));
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = userService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        var session = authService.login(request);
        AuthResponse response = new AuthResponse(session.expiresIn());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        authCookieService.createSessionCookie(session.token(), session.expiresIn()).toString())
                .body(response);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, authCookieService.clearSessionCookie().toString())
                .build();
    }
}
