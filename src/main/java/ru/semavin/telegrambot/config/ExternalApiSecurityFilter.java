package ru.semavin.telegrambot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import ru.semavin.telegrambot.services.auth.AccessTokenService;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

import java.io.IOException;
import java.util.Set;

/**
 * Production perimeter for externally reachable HTTP surfaces.
 * Controller and service authorization remains the second security layer.
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ExternalApiSecurityFilter extends OncePerRequestFilter {
    private static final Set<String> PUBLIC_API_PATHS = Set.of(
            "/api/v1/auth/telegram",
            "/api/v1/check",
            "/api/v1/schedule/semester/feed",
            "/api/v1/schedule/teacher/semester/feed"
    );

    private final AccessTokenService accessTokenService;
    private final HandlerExceptionResolver exceptionResolver;

    public ExternalApiSecurityFilter(
            AccessTokenService accessTokenService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver
    ) {
        this.accessTokenService = accessTokenService;
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        applySecurityHeaders(response);
        String path = applicationPath(request);

        if (isBlockedOperationalSurface(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (isProtectedApiPath(path)) {
            response.setHeader("Cache-Control", "private, no-store");
            try {
                accessTokenService.requireUserId(request.getHeader("Authorization"));
            } catch (TelegramAuthenticationException | AuthConfigurationException exception) {
                exceptionResolver.resolveException(request, response, null, exception);
                return;
            }
        } else if ("/api/v1/auth/telegram".equals(path)) {
            response.setHeader("Cache-Control", "private, no-store");
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    private boolean isProtectedApiPath(String path) {
        return path.startsWith("/api/v1/") && !PUBLIC_API_PATHS.contains(path);
    }

    private boolean isBlockedOperationalSurface(String path) {
        if (path.equals("/actuator/health")) {
            return false;
        }
        return path.equals("/actuator")
                || path.startsWith("/actuator/")
                || path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.equals("/swagger-ui")
                || path.startsWith("/swagger-ui/");
    }

    private String applicationPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return contextPath.isEmpty() ? requestUri : requestUri.substring(contextPath.length());
    }

    private void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader(
                "Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()"
        );
    }
}
