package ru.semavin.telegrambot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorsConfig {

    @Bean
    public Executor executor() {
        return Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("v", 1).factory()
        );
    }

}
