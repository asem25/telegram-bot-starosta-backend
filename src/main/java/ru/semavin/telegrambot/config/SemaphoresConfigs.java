package ru.semavin.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Semaphore;

@Configuration
public class SemaphoresConfigs {

    @Value("${maximum.group.parallel}")
    private int maximumParallelGroup;

    @Value("${maximum.dayparse.parallel}")
    private int maximumParallelDayParse;

    @Bean("groupSemaphore")
    public Semaphore groupSemaphore() {
        return new Semaphore(maximumParallelGroup);
    }

    @Bean("dayParseSemaphore")
    public Semaphore dayParseSemaphore() {
        return new Semaphore(maximumParallelDayParse);
    }

}
