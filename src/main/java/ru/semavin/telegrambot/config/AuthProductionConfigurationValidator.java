package ru.semavin.telegrambot.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ru.semavin.telegrambot.services.auth.AccessTokenService;
import ru.semavin.telegrambot.services.auth.TelegramAuthService;

/**
 * Production must fail fast instead of reporting healthy while authentication
 * is unusable. Local development and existing non-production tests may run
 * without Telegram secrets; requests still receive a controlled 503 response.
 */
@Component
@Profile({"production", "prod"})
@RequiredArgsConstructor
public class AuthProductionConfigurationValidator implements ApplicationRunner {
    private final TelegramAuthService telegramAuthService;
    private final AccessTokenService accessTokenService;

    @Override
    public void run(ApplicationArguments args) {
        telegramAuthService.validateConfiguration();
        accessTokenService.validateConfiguration();
    }
}
