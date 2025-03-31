package ru.semavin.telegrambot.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.GroupService;
import ru.semavin.telegrambot.services.schedules.ScheduleParserService;
import ru.semavin.telegrambot.services.schedules.SemesterService;

@TestConfiguration
public class TestConfig {
    @Bean
    public ScheduleRepository scheduleRepository() {
        return Mockito.mock(ScheduleRepository.class);
    }

    @Bean
    public GroupService groupService() {
        return Mockito.mock(GroupService.class);
    }

    @Bean
    public SemesterService semesterService() {
        return Mockito.mock(SemesterService.class);
    }

    @Bean
    public ScheduleMapper scheduleMapper() {
        return Mockito.mock(ScheduleMapper.class);
    }

    @Bean
    public ScheduleParserService scheduleParserService() {
        return Mockito.mock(ScheduleParserService.class);
    }


}
