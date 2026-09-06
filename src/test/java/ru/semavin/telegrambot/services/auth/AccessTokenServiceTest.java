package ru.semavin.telegrambot.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessTokenServiceTest {
    private AccessTokenService service;

    @BeforeEach
    void setUp() {
        service = new AccessTokenService();
        ReflectionTestUtils.setField(service, "tokenSecret", "test-secret-that-is-at-least-32-characters");
        ReflectionTestUtils.setField(service, "accessTokenTtlSeconds", 3600L);
    }

    @Test
    void issuesAndValidatesToken() {
        long internalUserId = 73L;
        AccessTokenService.IssuedToken token = service.issue(internalUserId);

        assertEquals(internalUserId, service.requireUserId("Bearer " + token.value()));
        assertEquals(3600L, token.expiresInSeconds());
        String encodedPayload = token.value().substring(0, token.value().indexOf('.'));
        String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
        assertEquals("v1:" + internalUserId, payload.substring(0, payload.lastIndexOf(':')));
    }

    @Test
    void rejectsTamperedToken() {
        AccessTokenService.IssuedToken token = service.issue(123456789L);

        assertThrows(
                TelegramAuthenticationException.class,
                () -> service.requireUserId("Bearer " + token.value() + "changed"));
    }

    @Test
    void reportsUnavailableWhenSecretIsNotConfigured() {
        ReflectionTestUtils.setField(service, "tokenSecret", "");

        assertThrows(AuthConfigurationException.class, () -> service.issue(123456789L));
    }

    @Test
    void reportsUnavailableWhenTtlIsInvalid() {
        ReflectionTestUtils.setField(service, "accessTokenTtlSeconds", 0L);

        assertThrows(AuthConfigurationException.class, () -> service.issue(123456789L));
    }
}
