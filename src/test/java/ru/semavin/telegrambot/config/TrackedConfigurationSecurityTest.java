package ru.semavin.telegrambot.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackedConfigurationSecurityTest {

    @Test
    void trackedConfigurationUsesEnvironmentPlaceholdersForSecrets() throws IOException {
        Properties common = load("application.properties");
        Properties local = load("application-local.properties");
        Properties production = load("application-prod.properties");

        assertEquals("${SPRING_DATASOURCE_URL}", common.getProperty("spring.datasource.url"));
        assertEquals("${SPRING_DATASOURCE_USERNAME}", common.getProperty("spring.datasource.username"));
        assertEquals("${SPRING_DATASOURCE_PASSWORD}", common.getProperty("spring.datasource.password"));
        assertEquals("${TELEGRAM_BOT_TOKEN:}", common.getProperty("telegram.bot.token"));
        assertEquals("${TELEGRAM_AUTH_TOKEN_SECRET:}", common.getProperty("telegram.auth.token-secret"));
        assertEquals("local", common.getProperty("spring.profiles.default"));
        assertEquals("optional:file:./test.env[.properties]",
                local.getProperty("spring.config.import"));

        assertEquals("${TELEGRAM_BOT_TOKEN}", production.getProperty("telegram.bot.token"));
        assertEquals("${TELEGRAM_AUTH_TOKEN_SECRET}",
                production.getProperty("telegram.auth.token-secret"));
        assertEquals("${MINI_APP_ALLOWED_ORIGINS}",
                production.getProperty("mini-app.allowed-origins"));
    }

    private Properties load(String resource) throws IOException {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            if (input == null) {
                throw new IllegalStateException("Missing test resource: " + resource);
            }
            properties.load(input);
        }
        return properties;
    }
}
