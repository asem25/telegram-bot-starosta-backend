package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.services.UserService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SchedulerCalendarISCServiceTest {
    private final ScheduleService scheduleService = mock(ScheduleService.class);
    private final ScheduleSyncStatusService syncStatusService = mock(ScheduleSyncStatusService.class);
    private final SchedulerCalendarISCService service =
            new SchedulerCalendarISCService(scheduleService, mock(UserService.class), syncStatusService);

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

    @Test
    void lastUpdateBadgeIsDisabledByDefault() {
        when(scheduleService.getScheduleForISC("GROUP-1")).thenReturn(List.of());
        when(syncStatusService.getLastSuccessfulSync("GROUP-1"))
                .thenReturn(java.util.Optional.of(Instant.parse("2026-09-06T11:30:00Z")));

        assertThat(service.getIscCalendarByGroupName("GROUP-1"))
                .doesNotContain("Обновлено с сайта МАИ");
    }

    @Test
    void enabledLastUpdateBadgeIsTransparentAndUsesStableUid() {
        when(scheduleService.getScheduleForISC("GROUP-1")).thenReturn(List.of());
        when(syncStatusService.getLastSuccessfulSync("GROUP-1"))
                .thenReturn(java.util.Optional.of(Instant.parse("2026-09-06T11:30:00Z")));

        String first = service.getIscCalendarByGroupName("GROUP-1", true);
        String second = service.getIscCalendarByGroupName("GROUP-1", true);

        assertThat(first)
                .contains("SUMMARY:Обновлено с сайта МАИ · 06.09 14:30")
                .contains("DTSTART;VALUE=DATE:20260906")
                .contains("DTEND;VALUE=DATE:20260907")
                .contains("TRANSP:TRANSPARENT")
                .containsPattern("UID:[0-9a-f-]+@starosta");
        assertThat(second).isEqualTo(first);
    }
}
