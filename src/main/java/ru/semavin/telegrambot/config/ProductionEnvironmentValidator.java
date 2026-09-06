package ru.semavin.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

@Component
@Profile({"production", "prod"})
public class ProductionEnvironmentValidator implements ApplicationRunner {

    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String telegramBotToken;
    private final String accessTokenSecret;
    private final long initDataMaxAgeSeconds;
    private final long accessTokenTtlSeconds;
    private final String allowedOrigins;
    private final String semesterStart;
    private final String semesterEnd;

    public ProductionEnvironmentValidator(
            @Value("${spring.datasource.url}") String datasourceUrl,
            @Value("${spring.datasource.username}") String datasourceUsername,
            @Value("${spring.datasource.password}") String datasourcePassword,
            @Value("${telegram.bot.token}") String telegramBotToken,
            @Value("${telegram.auth.token-secret}") String accessTokenSecret,
            @Value("${telegram.auth.init-data-max-age-seconds}") long initDataMaxAgeSeconds,
            @Value("${telegram.auth.access-token-ttl-seconds}") long accessTokenTtlSeconds,
            @Value("${mini-app.allowed-origins}") String allowedOrigins,
            @Value("${semester.start}") String semesterStart,
            @Value("${semester.end}") String semesterEnd
    ) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.telegramBotToken = telegramBotToken;
        this.accessTokenSecret = accessTokenSecret;
        this.initDataMaxAgeSeconds = initDataMaxAgeSeconds;
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.allowedOrigins = allowedOrigins;
        this.semesterStart = semesterStart;
        this.semesterEnd = semesterEnd;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:postgresql://")) {
            throw invalid("SPRING_DATASOURCE_URL");
        }
        requireNonBlank(datasourceUsername, "SPRING_DATASOURCE_USERNAME");
        requireNonBlank(datasourcePassword, "SPRING_DATASOURCE_PASSWORD");
        requireNonBlank(telegramBotToken, "TELEGRAM_BOT_TOKEN");
        if (accessTokenSecret == null
                || accessTokenSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw invalid("TELEGRAM_AUTH_TOKEN_SECRET");
        }
        if (initDataMaxAgeSeconds <= 0) {
            throw invalid("TELEGRAM_INIT_DATA_MAX_AGE_SECONDS");
        }
        if (accessTokenTtlSeconds <= 0) {
            throw invalid("TELEGRAM_ACCESS_TOKEN_TTL_SECONDS");
        }
        validateOrigins();
        validateSemester();
    }

    private void validateOrigins() {
        requireNonBlank(allowedOrigins, "MINI_APP_ALLOWED_ORIGINS");
        boolean valid = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .allMatch(this::isHttpOrigin);
        if (!valid) {
            throw invalid("MINI_APP_ALLOWED_ORIGINS");
        }
    }

    private boolean isHttpOrigin(String value) {
        try {
            URI origin = URI.create(value);
            return origin.getHost() != null
                    && ("http".equalsIgnoreCase(origin.getScheme())
                    || "https".equalsIgnoreCase(origin.getScheme()));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void validateSemester() {
        try {
            LocalDate start = LocalDate.parse(semesterStart);
            LocalDate end = LocalDate.parse(semesterEnd);
            if (start.isAfter(end)) {
                throw invalid("SEMESTER_START/SEMESTER_END");
            }
        } catch (DateTimeParseException | NullPointerException exception) {
            throw invalid("SEMESTER_START/SEMESTER_END");
        }
    }

    private void requireNonBlank(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw invalid(variable);
        }
    }

    private IllegalStateException invalid(String variable) {
        return new IllegalStateException("Invalid required production variable: " + variable);
    }
}
