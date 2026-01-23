package ru.semavin.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class SemaphoresConfigs {

    @Value("${database.maximum.parallel}")
    private int maximumParallel;

    @Bean
    public Semaphore semaphore() {
        return new Semaphore(maximumParallel);
    }

}
