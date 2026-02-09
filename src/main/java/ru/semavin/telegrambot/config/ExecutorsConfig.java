package ru.semavin.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ExecutorsConfig {


    @Value("${database.maximum.parallel}")
    private int maximumParallel;

    @Bean
    public Executor executor() {
        return Executors.newFixedThreadPool(maximumParallel);
    }

}
