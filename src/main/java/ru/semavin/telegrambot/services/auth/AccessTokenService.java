package ru.semavin.telegrambot.services.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AccessTokenService {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final String BEARER_PREFIX = "Bearer ";

    private final Clock clock = Clock.systemUTC();

    @Value("${telegram.auth.token-secret:}")
    private String tokenSecret;

    @Value("${telegram.auth.access-token-ttl-seconds:3600}")
    private long accessTokenTtlSeconds;

    public IssuedToken issue(long telegramId) {
        validateConfiguration();
        long expiresAt;
        try {
            expiresAt = Math.addExact(Instant.now(clock).getEpochSecond(), accessTokenTtlSeconds);
        } catch (ArithmeticException exception) {
            throw new AuthConfigurationException("Время жизни access token настроено некорректно");
        }
        String payload = telegramId + ":" + expiresAt;
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return new IssuedToken(encodedPayload + "." + sign(encodedPayload), accessTokenTtlSeconds);
    }

    public long requireTelegramId(String authorizationHeader) {
        validateConfiguration();
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw unauthorized("Требуется Bearer-токен");
        }

        String[] parts = authorizationHeader.substring(BEARER_PREFIX.length()).split("\\.", 2);
        if (parts.length != 2 || !constantTimeEquals(sign(parts[0]), parts[1])) {
            throw unauthorized("Недействительный access token");
        }

        try {
            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String[] values = payload.split(":", 2);
            long telegramId = Long.parseLong(values[0]);
            long expiresAt = Long.parseLong(values[1]);
            if (Instant.now(clock).getEpochSecond() >= expiresAt) {
                throw unauthorized("Срок действия access token истёк");
            }
            return telegramId;
        } catch (TelegramAuthenticationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw unauthorized("Некорректный access token");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(tokenSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA_256));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось подписать access token", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
    }

    public void validateConfiguration() {
        if (tokenSecret == null || tokenSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new AuthConfigurationException("Сервис access token не настроен");
        }
        if (accessTokenTtlSeconds <= 0) {
            throw new AuthConfigurationException("Время жизни access token настроено некорректно");
        }
    }

    private TelegramAuthenticationException unauthorized(String message) {
        return new TelegramAuthenticationException(message);
    }

    public record IssuedToken(String value, long expiresInSeconds) {
    }
}
