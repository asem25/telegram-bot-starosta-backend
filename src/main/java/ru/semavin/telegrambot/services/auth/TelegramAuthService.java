package ru.semavin.telegrambot.services.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.TelegramAuthResponse;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.NotificationPreferencesService;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramAuthService {
    private static final String HMAC_SHA_256 = "HmacSHA256";
    private static final byte[] WEB_APP_DATA_KEY = "WebAppData".getBytes(StandardCharsets.UTF_8);

    private final UserService userService;
    private final AccessTokenService accessTokenService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final Gson gson = new Gson();
    private final Clock clock = Clock.systemUTC();

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.auth.init-data-max-age-seconds:300}")
    private long initDataMaxAgeSeconds;

    public TelegramAuthResponse authenticate(String initData) {
        validateConfiguration();
        Map<String, String> values = parse(initData);
        validateSignature(values);
        validateAge(values.get("auth_date"));

        JsonObject telegramUser = parseTelegramUser(values.get("user"));
        long telegramId = requiredLong(telegramUser, "id");
        String username = optionalString(telegramUser, "username");
        String firstName = requiredString(telegramUser, "first_name");
        String lastName = optionalString(telegramUser, "last_name");
        UserDTO user = userService.provisionTelegramUser(
                telegramId,
                username,
                firstName,
                lastName);
        notificationPreferencesService.synchronizeTelegramWriteAccess(
                telegramId,
                optionalBoolean(telegramUser, "allows_write_to_pm"));
        AccessTokenService.IssuedToken token = accessTokenService.issue(telegramId);
        return new TelegramAuthResponse(token.value(), token.expiresInSeconds(), user);
    }

    private Map<String, String> parse(String initData) {
        if (initData == null || initData.isBlank()) {
            throw unauthorized("Telegram initData отсутствует");
        }
        try {
            return Arrays.stream(initData.split("&"))
                    .map(part -> part.split("=", 2))
                    .filter(parts -> parts.length == 2)
                    .collect(Collectors.toMap(
                            parts -> decode(parts[0]),
                            parts -> decode(parts[1]),
                            (first, ignored) -> first));
        } catch (RuntimeException exception) {
            throw unauthorized("Некорректный формат Telegram initData");
        }
    }

    private void validateSignature(Map<String, String> values) {
        String receivedHash = values.get("hash");
        if (receivedHash == null || receivedHash.isBlank()) {
            throw unauthorized("Подпись Telegram initData отсутствует");
        }

        String dataCheckString = values.entrySet().stream()
                .filter(entry -> !"hash".equals(entry.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("\n"));

        byte[] secretKey = hmac(WEB_APP_DATA_KEY, botToken.getBytes(StandardCharsets.UTF_8));
        String calculatedHash = HexFormat.of().formatHex(
                hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        if (!MessageDigest.isEqual(
                calculatedHash.getBytes(StandardCharsets.US_ASCII),
                receivedHash.toLowerCase().getBytes(StandardCharsets.US_ASCII))) {
            throw unauthorized("Подпись Telegram initData недействительна");
        }
    }

    private void validateAge(String authDateValue) {
        long authDate;
        try {
            authDate = Long.parseLong(authDateValue);
        } catch (RuntimeException exception) {
            throw unauthorized("Telegram auth_date отсутствует или некорректен");
        }

        long now = Instant.now(clock).getEpochSecond();
        if (authDate > now + 30 || now - authDate > initDataMaxAgeSeconds) {
            throw unauthorized("Срок действия Telegram initData истёк");
        }
    }

    private JsonObject parseTelegramUser(String userJson) {
        if (userJson == null || userJson.isBlank()) {
            throw unauthorized("Telegram user отсутствует в initData");
        }
        try {
            return gson.fromJson(userJson, JsonObject.class);
        } catch (RuntimeException exception) {
            throw unauthorized("Некорректные данные Telegram user");
        }
    }

    private long requiredLong(JsonObject object, String field) {
        try {
            return object.get(field).getAsLong();
        } catch (RuntimeException exception) {
            throw unauthorized("Telegram user не содержит идентификатор");
        }
    }

    private String requiredString(JsonObject object, String field) {
        String value = optionalString(object, field);
        if (value == null || value.isBlank()) {
            throw unauthorized("Telegram user не содержит обязательное поле профиля");
        }
        return value;
    }

    private String optionalString(JsonObject object, String field) {
        try {
            if (!object.has(field) || object.get(field).isJsonNull()) {
                return null;
            }
            String value = object.get(field).getAsString().trim();
            return value.isEmpty() ? null : value;
        } catch (RuntimeException exception) {
            throw unauthorized("Некорректные данные профиля Telegram user");
        }
    }

    private boolean optionalBoolean(JsonObject object, String field) {
        try {
            return object.has(field) && !object.get(field).isJsonNull() && object.get(field).getAsBoolean();
        } catch (RuntimeException exception) {
            throw unauthorized("Некорректные данные разрешений Telegram user");
        }
    }

    private byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA_256);
            mac.init(new SecretKeySpec(key, HMAC_SHA_256));
            return mac.doFinal(data);
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось проверить подпись Telegram", exception);
        }
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private TelegramAuthenticationException unauthorized(String message) {
        return new TelegramAuthenticationException(message);
    }

    public void validateConfiguration() {
        if (botToken == null || botToken.isBlank()) {
            throw new AuthConfigurationException("Сервис Telegram-авторизации не настроен");
        }
        if (initDataMaxAgeSeconds <= 0) {
            throw new AuthConfigurationException("Время жизни Telegram initData настроено некорректно");
        }
    }
}
