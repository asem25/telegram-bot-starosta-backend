package ru.semavin.telegrambot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.DeadlineDTO;
import ru.semavin.telegrambot.dto.MiniAppDeadlineResponse;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.dto.NotificationSettingsResponse;
import ru.semavin.telegrambot.dto.UpdateNotificationSettingsRequest;
import ru.semavin.telegrambot.dto.NotificationHistoryResponse;
import ru.semavin.telegrambot.dto.GroupStudentAbsenceResponse;
import ru.semavin.telegrambot.dto.StudentAbsenceRequest;
import ru.semavin.telegrambot.dto.StudentAbsenceResponse;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.StarostaService;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.StudentAbsenceService;
import ru.semavin.telegrambot.services.NotificationPreferencesService;
import ru.semavin.telegrambot.services.auth.AccessTokenService;
import ru.semavin.telegrambot.services.deadline.DeadlineService;
import ru.semavin.telegrambot.services.schedules.ScheduleService;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MiniAppControllerTest {
    private AccessTokenService accessTokenService;
    private UserService userService;
    private ScheduleService scheduleService;
    private DeadlineService deadlineService;
    private StarostaService starostaService;
    private ScheduleChangeService scheduleChangeService;
    private StudentAbsenceService studentAbsenceService;
    private NotificationPreferencesService notificationPreferencesService;
    private MiniAppController controller;

    @BeforeEach
    void setUp() {
        accessTokenService = mock(AccessTokenService.class);
        userService = mock(UserService.class);
        scheduleService = mock(ScheduleService.class);
        deadlineService = mock(DeadlineService.class);
        starostaService = mock(StarostaService.class);
        scheduleChangeService = mock(ScheduleChangeService.class);
        studentAbsenceService = mock(StudentAbsenceService.class);
        notificationPreferencesService = mock(NotificationPreferencesService.class);
        controller = new MiniAppController(
                accessTokenService,
                userService,
                scheduleService,
                deadlineService,
                starostaService,
                scheduleChangeService,
                studentAbsenceService,
                notificationPreferencesService);

        when(accessTokenService.requireUserId("Bearer token")).thenReturn(73L);
        when(userService.getUserById(73L)).thenReturn(UserDTO.builder()
                .groupName("GROUP-1")
                .build());
        when(scheduleService.getScheduleForDay(anyString(), anyString())).thenReturn(List.of());
    }

    @Test
    void acceptsExactlyThirtyOneInclusiveDays() {
        AtomicInteger calls = new AtomicInteger();
        when(scheduleService.getScheduleForDay(anyString(), anyString())).thenAnswer(invocation -> {
            calls.incrementAndGet();
            return List.of();
        });

        controller.getMySchedule(
                "Bearer token",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 1));

        assertEquals(31, calls.get());
    }

    @Test
    void rejectsThirtyTwoInclusiveDaysBeforeScheduleLookup() {
        assertThrows(MiniAppRequestException.class, () -> controller.getMySchedule(
                "Bearer token",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 10, 2)));

        verify(scheduleService, never()).getScheduleForDay(anyString(), anyString());
    }

    @Test
    void handlesMaximumLocalDateWithoutOverflow() {
        controller.getMySchedule("Bearer token", LocalDate.MAX, LocalDate.MAX);

        verify(scheduleService).getScheduleForDay(org.mockito.ArgumentMatchers.eq("GROUP-1"), anyString());
    }

    @Test
    void browserDeadlineDoesNotExposeRecipientsOrDeliveryState() {
        UUID deadlineId = UUID.randomUUID();
        when(deadlineService.getAllByGroup("GROUP-1")).thenReturn(List.of(DeadlineDTO.builder()
                .uuid(deadlineId)
                .title("Task")
                .description("Description")
                .dueDate(LocalDate.of(2026, 9, 10))
                .creatorId(73L)
                .receiverIds(List.of(74L))
                .notified1Day(true)
                .build()));

        List<MiniAppDeadlineResponse> body = controller.getMyDeadlines("Bearer token").getBody();

        assertEquals(1, body.size());
        assertEquals(deadlineId, body.getFirst().uuid());
        assertEquals(4, MiniAppDeadlineResponse.class.getRecordComponents().length);
    }

    @Test
    void claimsStarostaForBearerIdentityOnly() {
        UserDTO expected = UserDTO.builder().role("STAROSTA").build();
        when(starostaService.claimOwnGroup(73L)).thenReturn(expected);

        assertEquals(expected, controller.claimStarosta("Bearer token").getBody());

        verify(accessTokenService).requireUserId("Bearer token");
        verify(starostaService).claimOwnGroup(73L);
    }

    @Test
    void releasesStarostaForBearerIdentityOnly() {
        UserDTO expected = UserDTO.builder().role("STUDENT").build();
        when(starostaService.releaseOwnGroup(73L)).thenReturn(expected);

        assertEquals(expected, controller.releaseStarosta("Bearer token").getBody());

        verify(accessTokenService).requireUserId("Bearer token");
        verify(starostaService).releaseOwnGroup(73L);
    }

    @Test
    void absenceCrudUsesBearerIdentityAndLocalDates() {
        StudentAbsenceRequest request = new StudentAbsenceRequest(
                LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 4), "Причина");
        StudentAbsenceResponse expected = new StudentAbsenceResponse(
                7L, request.startDate(), request.endDate(), request.reason());
        when(studentAbsenceService.create(73L, request)).thenReturn(expected);

        var created = controller.createAbsence("Bearer token", request);
        var listed = controller.getOwnAbsences("Bearer token");
        var deleted = controller.deleteOwnAbsence("Bearer token", 7L);

        assertEquals(201, created.getStatusCode().value());
        assertEquals(expected, created.getBody());
        assertEquals(200, listed.getStatusCode().value());
        assertEquals(204, deleted.getStatusCode().value());
        assertNull(deleted.getBody());
        verify(studentAbsenceService).getOwn(73L);
        verify(studentAbsenceService).deleteOwn(73L, 7L);
    }

    @Test
    void groupAbsencesAreDelegatedWithBearerIdentityOnly() {
        List<GroupStudentAbsenceResponse> expected = List.of(new GroupStudentAbsenceResponse(
                8L, 2L, LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 2), null));
        when(studentAbsenceService.getOwnGroup(73L)).thenReturn(expected);

        assertEquals(expected, controller.getOwnGroupAbsences("Bearer token").getBody());

        verify(studentAbsenceService).getOwnGroup(73L);
    }

    @Test
    void readsAndUpdatesNotificationSettingsForBearerIdentity() {
        NotificationSettingsResponse initial = new NotificationSettingsResponse(true, true, false);
        UpdateNotificationSettingsRequest request = new UpdateNotificationSettingsRequest(false, true, true);
        NotificationSettingsResponse updated = new NotificationSettingsResponse(false, true, true);
        when(notificationPreferencesService.getSettings(73L)).thenReturn(initial);
        when(notificationPreferencesService.updateSettings(73L, request)).thenReturn(updated);

        assertEquals(initial, controller.getNotificationSettings("Bearer token").getBody());
        assertEquals(updated, controller.updateNotificationSettings("Bearer token", request).getBody());

        verify(notificationPreferencesService).getSettings(73L);
        verify(notificationPreferencesService).updateSettings(73L, request);
    }

    @Test
    void returnsPersistedHistoryEvenWhenTelegramDeliveryFailed() {
        NotificationHistoryResponse failed = new NotificationHistoryResponse(
                UUID.randomUUID(),
                "SCHEDULE_CHANGE",
                "Расписание изменено",
                "Изменена аудитория",
                java.time.OffsetDateTime.parse("2026-08-30T10:15:30+03:00"),
                "TEMPORARY_FAILURE");
        when(notificationPreferencesService.getHistory(73L)).thenReturn(List.of(failed));

        assertEquals(List.of(failed), controller.getNotificationHistory("Bearer token").getBody());
        verify(notificationPreferencesService).getHistory(73L);
    }

}
