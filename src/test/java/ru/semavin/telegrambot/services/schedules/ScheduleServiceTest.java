package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.groups.GroupService;

class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleParserService scheduleParserService;
    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private SemesterService semesterService;
    @Mock
    private GroupService groupService;
    @Mock
    private ScheduleChangeService scheduleChangeService;

    @Test
    void getActualSchedule() {
    }
}