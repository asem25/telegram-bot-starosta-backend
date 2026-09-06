package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.services.UserService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerCalendarISCServiceTest {
    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final SchedulerCalendarISCService service =
            new SchedulerCalendarISCService(scheduleService, mock(UserService.class));

    @Test
    void groupFeedUsesStableOccurrenceUidAndMovedTime() {
        UUID occurrenceId = UUID.randomUUID();
        when(scheduleService.getScheduleForISC("GROUP-1")).thenReturn(List.of(
                ScheduleDTO.builder()
                        .lessonOccurrenceId(occurrenceId)
                        .groupName("GROUP-1")
                        .subjectName("Math")
                        .lessonType("LECTURE")
                        .lessonDate(LocalDate.of(2026, 9, 2))
                        .startTime(LocalTime.of(11, 0))
                        .endTime(LocalTime.of(12, 30))
                        .build()
        ));

        String calendar = service.getIscCalendarByGroupName("GROUP-1");

        assertThat(calendar)
                .contains("UID:" + occurrenceId + "@starosta")
                .contains("DTSTART;TZID=Europe/Moscow:20260902T110000")
                .contains("DTEND;TZID=Europe/Moscow:20260902T123000");
    }

    @Test
    void cancelledLessonIsAbsentWhenMergedScheduleIsEmpty() {
        when(scheduleService.getScheduleForISC("GROUP-1")).thenReturn(List.of());

        assertThat(service.getIscCalendarByGroupName("GROUP-1"))
                .doesNotContain("BEGIN:VEVENT");
    }
}
