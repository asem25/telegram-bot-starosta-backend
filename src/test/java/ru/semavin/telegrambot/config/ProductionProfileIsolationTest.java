package ru.semavin.telegrambot.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProductionProfileIsolationTest {

    @Test
    void productionProfileDoesNotImportLocalTestEnvironmentFile() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    assertNull(context.getEnvironment().getProperty("spring.config.import"));
                    assertEquals("prod",
                            context.getEnvironment().getProperty("management.metrics.tags.env"));
                });
    }
}
