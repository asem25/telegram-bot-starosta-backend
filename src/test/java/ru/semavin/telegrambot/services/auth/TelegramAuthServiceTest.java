package ru.semavin.telegrambot.services.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.semavin.telegrambot.dto.TelegramAuthResponse;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.NotificationPreferencesService;
import ru.semavin.telegrambot.utils.exceptions.TelegramAuthenticationException;
import ru.semavin.telegrambot.utils.exceptions.AuthConfigurationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramAuthServiceTest {
    private static final String BOT_TOKEN = "123456:test-token";

    private UserService userService;
    private AccessTokenService accessTokenService;
    private NotificationPreferencesService notificationPreferencesService;
    private TelegramAuthService service;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        accessTokenService = mock(AccessTokenService.class);
        notificationPreferencesService = mock(NotificationPreferencesService.class);
        service = new TelegramAuthService(userService, accessTokenService, notificationPreferencesService);
        ReflectionTestUtils.setField(service, "botToken", BOT_TOKEN);
        ReflectionTestUtils.setField(service, "initDataMaxAgeSeconds", 300L);
    }

    @Test
    void ignoresTelegramProfileFieldsAndReturnsOnlySafeUserJson() throws Exception {
        long telegramId = 987654321L;
        long internalUserId = 73L;
        UserDTO user = UserDTO.builder()
                .role("STUDENT")
                .groupName("TEST-GROUP")
                .build();
        when(userService.provisionTelegramUser(telegramId))
                .thenReturn(new UserService.ProvisionedUser(internalUserId, user));
        when(accessTokenService.issue(internalUserId))
                .thenReturn(new AccessTokenService.IssuedToken("access-token", 3600));

        TelegramAuthResponse response = service.authenticate(validInitData(
                telegramId, "ignored_username", "IgnoredFirst", "IgnoredLast"));

        assertEquals("access-token", response.accessToken());
        assertEquals("STUDENT", response.user().getRole());
        assertEquals("TEST-GROUP", response.user().getGroupName());
        JsonNode userJson = new ObjectMapper().valueToTree(response).get("user");
        assertFalse(userJson.has("telegramId"));
        assertFalse(userJson.has("username"));
        assertFalse(userJson.has("firstName"));
        assertFalse(userJson.has("lastName"));
        assertFalse(userJson.has("patronymic"));
        verify(userService).provisionTelegramUser(telegramId);
        verify(accessTokenService).issue(internalUserId);
        verify(notificationPreferencesService).synchronizeTelegramWriteAccess(telegramId, false);
    }

    @Test
    void refreshesExistingUserThroughTheSameAtomicProvisioningPath() throws Exception {
        long telegramId = 987654322L;
        long internalUserId = 74L;
        UserDTO existingUser = UserDTO.builder()
                .role("STAROSTA")
                .groupName("TEST-GROUP")
                .build();
        when(userService.provisionTelegramUser(telegramId))
                .thenReturn(new UserService.ProvisionedUser(internalUserId, existingUser));
        when(accessTokenService.issue(internalUserId))
                .thenReturn(new AccessTokenService.IssuedToken("access-token", 3600));

        TelegramAuthResponse response = service.authenticate(validInitData(
                telegramId, "updated_name", "Updated", "Profile"));

        assertEquals("STAROSTA", response.user().getRole());
        assertEquals("TEST-GROUP", response.user().getGroupName());
        verify(userService).provisionTelegramUser(telegramId);
        verify(accessTokenService).issue(internalUserId);
    }

    @Test
    void acceptsTelegramUserWithoutAnyProfileFields() throws Exception {
        long telegramId = 987654323L;
        long internalUserId = 75L;
        UserDTO user = UserDTO.builder().build();
        when(userService.provisionTelegramUser(telegramId))
                .thenReturn(new UserService.ProvisionedUser(internalUserId, user));
        when(accessTokenService.issue(internalUserId))
                .thenReturn(new AccessTokenService.IssuedToken("access-token", 3600));

        TelegramAuthResponse response = service.authenticate(validInitData(
                telegramId, null, null, null));

        assertEquals("STUDENT", response.user().getRole());
        verify(userService).provisionTelegramUser(telegramId);
        verify(accessTokenService).issue(internalUserId);
    }

    @Test
    void rejectsInvalidSignature() {
        String initData = "auth_date=" + Instant.now().getEpochSecond()
                + "&user=%7B%22id%22%3A987654321%7D&hash=invalid";

        assertThrows(TelegramAuthenticationException.class, () -> service.authenticate(initData));
        verify(userService, never()).provisionTelegramUser(
                org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void reportsUnavailableWhenBotTokenIsNotConfigured() {
        ReflectionTestUtils.setField(service, "botToken", "");

        assertThrows(AuthConfigurationException.class, () -> service.authenticate("invalid"));
    }

    @Test
    void reportsUnavailableWhenInitDataLifetimeIsInvalid() {
        ReflectionTestUtils.setField(service, "initDataMaxAgeSeconds", 0L);

        assertThrows(AuthConfigurationException.class, () -> service.authenticate("invalid"));
    }

    private String validInitData(
            long telegramId,
            String username,
            String firstName,
            String lastName
    ) throws Exception {
        String authDate = String.valueOf(Instant.now().getEpochSecond());
        StringBuilder userJsonBuilder = new StringBuilder("{\"id\":")
                .append(telegramId);
        if (firstName != null) {
            userJsonBuilder.append(",\"first_name\":\"").append(firstName).append("\"");
        }
        if (lastName != null) {
            userJsonBuilder.append(",\"last_name\":\"").append(lastName).append("\"");
        }
        if (username != null) {
            userJsonBuilder.append(",\"username\":\"").append(username).append("\"");
        }
        String userJson = userJsonBuilder.append('}').toString();
        String dataCheckString = "auth_date=" + authDate + "\nuser=" + userJson;

        byte[] secretKey = hmac("WebAppData".getBytes(StandardCharsets.UTF_8),
                BOT_TOKEN.getBytes(StandardCharsets.UTF_8));
        String hash = HexFormat.of().formatHex(
                hmac(secretKey, dataCheckString.getBytes(StandardCharsets.UTF_8)));

        return "auth_date=" + authDate
                + "&user=" + URLEncoder.encode(userJson, StandardCharsets.UTF_8)
                + "&hash=" + hash;
    }

    private byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }
}
