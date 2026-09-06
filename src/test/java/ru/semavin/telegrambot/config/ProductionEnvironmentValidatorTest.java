package ru.semavin.telegrambot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProductionEnvironmentValidatorTest {

    private static final String VALID_SECRET = "a-secure-signing-secret-with-32-bytes";
    private static final ApplicationArguments ARGUMENTS = mock(ApplicationArguments.class);

    @Test
    void acceptsValidProductionEnvironment() {
        assertDoesNotThrow(() -> validValidator().run(ARGUMENTS));
    }

    @Test
    void rejectsMissingDatabasePassword() {
        ProductionEnvironmentValidator validator = validator(
                "jdbc:postgresql://database:5432/starosta", "app", "",
                "telegram-token", VALID_SECRET, 300, 3600,
                "https://app.example.com", "2026-09-01", "2027-01-31");

        assertThrows(IllegalStateException.class, () -> validator.run(ARGUMENTS));
    }

    @Test
    void rejectsShortAccessTokenSecret() {
        ProductionEnvironmentValidator validator = validator(
                "jdbc:postgresql://database:5432/starosta", "app", "password",
                "telegram-token", "too-short", 300, 3600,
                "https://app.example.com", "2026-09-01", "2027-01-31");

        assertThrows(IllegalStateException.class, () -> validator.run(ARGUMENTS));
    }

    @Test
    void rejectsInvalidOriginAndSemesterRange() {
        ProductionEnvironmentValidator invalidOrigin = validator(
                "jdbc:postgresql://database:5432/starosta", "app", "password",
                "telegram-token", VALID_SECRET, 300, 3600,
                "file:///tmp/app", "2026-09-01", "2027-01-31");
        ProductionEnvironmentValidator invalidSemester = validator(
                "jdbc:postgresql://database:5432/starosta", "app", "password",
                "telegram-token", VALID_SECRET, 300, 3600,
                "https://app.example.com", "2027-01-31", "2026-09-01");

        assertThrows(IllegalStateException.class, () -> invalidOrigin.run(ARGUMENTS));
        assertThrows(IllegalStateException.class, () -> invalidSemester.run(ARGUMENTS));
    }

    private ProductionEnvironmentValidator validValidator() {
        return validator(
                "jdbc:postgresql://database:5432/starosta", "app", "password",
                "telegram-token", VALID_SECRET, 300, 3600,
                "https://app.example.com,https://admin.example.com",
                "2026-09-01", "2027-01-31");
    }

    private ProductionEnvironmentValidator validator(
            String datasourceUrl,
            String datasourceUsername,
            String datasourcePassword,
            String telegramBotToken,
            String accessTokenSecret,
            long initDataMaxAgeSeconds,
            long accessTokenTtlSeconds,
            String allowedOrigins,
            String semesterStart,
            String semesterEnd
    ) {
        return new ProductionEnvironmentValidator(
                datasourceUrl, datasourceUsername, datasourcePassword,
                telegramBotToken, accessTokenSecret,
                initDataMaxAgeSeconds, accessTokenTtlSeconds,
                allowedOrigins, semesterStart, semesterEnd);
    }
}
