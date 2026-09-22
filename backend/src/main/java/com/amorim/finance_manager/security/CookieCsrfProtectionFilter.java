package com.amorim.finance_manager.security;

import com.amorim.finance_manager.config.CorsProperties;
import com.amorim.finance_manager.shared.exception.ApiError;
import com.amorim.finance_manager.shared.exception.ApiErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

/**
 * Protects state-changing requests authenticated with the browser session cookie.
 *
 * <p>The frontend and API are hosted on different origins, so the session cookie
 * needs {@code SameSite=None} in production. CORS alone does not protect an API
 * from a form POST made by another site. Browser requests carrying the session
 * cookie must therefore originate from one of the explicitly configured frontend
 * origins. Bearer-token clients remain stateless and are not subject to this check.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CookieCsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> UNSAFE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final CorsProperties corsProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !UNSAFE_METHODS.contains(request.getMethod())
                || !hasSessionCookie(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isTrustedRequestOrigin(request)) {
            log.warn(
                    "event=csrf.origin_rejected method={} path={} origin={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getHeader(HttpHeaders.ORIGIN)
            );

            ApiError error = ApiError.of(
                    HttpStatus.FORBIDDEN,
                    ApiErrorCode.CSRF_VALIDATION_FAILED,
                    "Origem da requisição não é permitida.",
                    request.getRequestURI()
            );

            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), error);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasSessionCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (AuthCookieService.SESSION_COOKIE_NAME.equals(cookie.getName())
                    && !cookie.getValue().isBlank()) {
                return true;
            }
        }

        return false;
    }

    private boolean isTrustedRequestOrigin(HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);

        if (origin != null) {
            return corsProperties.allowedOrigins().contains(origin);
        }

        return originFromReferer(request.getHeader(HttpHeaders.REFERER))
                .map(candidate -> corsProperties.allowedOrigins().contains(candidate))
                .orElse(false);
    }

    private java.util.Optional<String> originFromReferer(String referer) {
        if (referer == null || referer.isBlank()) {
            return java.util.Optional.empty();
        }

        try {
            URI uri = URI.create(referer);

            if (uri.getScheme() == null || uri.getHost() == null) {
                return java.util.Optional.empty();
            }

            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            return java.util.Optional.of(uri.getScheme() + "://" + uri.getHost() + port);
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
    }
}
