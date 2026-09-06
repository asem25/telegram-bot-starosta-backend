package ru.semavin.telegrambot.services.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

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
        AccessTokenService.IssuedToken token = service.issue(123456789L);

        assertEquals(123456789L, service.requireTelegramId("Bearer " + token.value()));
        assertEquals(3600L, token.expiresInSeconds());
    }

    @Test
    void rejectsTamperedToken() {
        AccessTokenService.IssuedToken token = service.issue(123456789L);

        assertThrows(
                TelegramAuthenticationException.class,
                () -> service.requireTelegramId("Bearer " + token.value() + "changed"));
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
