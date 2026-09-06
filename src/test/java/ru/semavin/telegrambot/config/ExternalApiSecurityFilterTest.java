package ru.semavin.telegrambot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import ru.semavin.telegrambot.services.auth.AccessTokenService;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ExternalApiSecurityFilterTest {
    private AccessTokenService accessTokenService;
    private AtomicReference<Exception> resolvedException;
    private ExternalApiSecurityFilter filter;
    private CountingFilterChain filterChain;

    @BeforeEach
    void setUp() {
        accessTokenService = new AccessTokenService();
        ReflectionTestUtils.setField(
                accessTokenService,
                "tokenSecret",
                "story-17-test-token-secret-at-least-32-bytes"
        );
        ReflectionTestUtils.setField(accessTokenService, "accessTokenTtlSeconds", 3600L);
        resolvedException = new AtomicReference<>();
        HandlerExceptionResolver exceptionResolver = (request, response, handler, exception) -> {
            resolvedException.set(exception);
            response.setStatus(401);
            return new ModelAndView();
        };
        filter = new ExternalApiSecurityFilter(accessTokenService, exceptionResolver);
        filterChain = new CountingFilterChain();
    }

    @Test
    void publicCalendarAndAuthenticationEndpointsRemainOpen() throws Exception {
        for (String path : new String[]{
                "/api/v1/auth/telegram",
                "/api/v1/schedule/semester/feed",
                "/api/v1/schedule/teacher/semester/feed"
        }) {
            filter.doFilter(request("GET", path), new MockHttpServletResponse(), filterChain);
        }

        assertEquals(3, filterChain.invocations());
    }

    @Test
    void protectedApiRequiresValidBearerBeforeControllerDispatch() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/me/schedule/changes");
        request.addHeader(
                "Authorization",
                "Bearer " + accessTokenService.issue(42L).value()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(1, filterChain.invocations());
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
    }

    @Test
    void invalidBearerStopsBeforeControllerAndUsesExistingErrorContract() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/me/deadlines");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(0, filterChain.invocations());
        assertEquals(401, response.getStatus());
        assertInstanceOf(TelegramAuthenticationException.class, resolvedException.get());
    }

    @Test
    void productionHidesDocumentationAndDetailedManagementEndpoints() throws Exception {
        for (String path : new String[]{
                "/swagger-ui/index.html",
                "/v3/api-docs",
                "/actuator/health/db",
                "/actuator/metrics",
                "/actuator/prometheus"
        }) {
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request("GET", path), response, filterChain);

            assertEquals(404, response.getStatus(), path);
        }

        assertEquals(0, filterChain.invocations());
    }

    @Test
    void healthChecksStayAvailableWithoutExposingApplicationData() throws Exception {
        for (String path : new String[]{"/api/v1/check", "/actuator/health"}) {
            filter.doFilter(request("GET", path), new MockHttpServletResponse(), filterChain);
        }

        assertEquals(2, filterChain.invocations());
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static final class CountingFilterChain implements FilterChain {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException {
            invocations.incrementAndGet();
        }

        int invocations() {
            return invocations.get();
        }
    }
}
