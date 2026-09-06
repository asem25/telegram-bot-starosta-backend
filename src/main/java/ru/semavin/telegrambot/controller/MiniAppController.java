package ru.semavin.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import ru.semavin.telegrambot.dto.GroupStudentAbsenceResponse;
import ru.semavin.telegrambot.dto.StudentAbsenceRequest;
import ru.semavin.telegrambot.dto.StudentAbsenceResponse;
import ru.semavin.telegrambot.dto.MiniAppDeadlineResponse;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.dto.MiniAppScheduleChangeRequest;
import ru.semavin.telegrambot.dto.MiniAppScheduleChangeResponse;
import ru.semavin.telegrambot.dto.NotificationHistoryResponse;
import ru.semavin.telegrambot.dto.NotificationSettingsResponse;
import ru.semavin.telegrambot.dto.UpdateNotificationSettingsRequest;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.StudentAbsenceService;
import ru.semavin.telegrambot.services.NotificationPreferencesService;
import jakarta.validation.Valid;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.StarostaService;
import ru.semavin.telegrambot.services.auth.AccessTokenService;
import ru.semavin.telegrambot.services.deadline.DeadlineService;
import ru.semavin.telegrambot.services.schedules.ScheduleService;
import ru.semavin.telegrambot.utils.DateUtils;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.LongStream;

@RestController
@RequestMapping("api/v1/me")
@RequiredArgsConstructor
public class MiniAppController {
    private static final long MAX_SCHEDULE_RANGE_DAYS = 31;

    private final AccessTokenService accessTokenService;
    private final UserService userService;
    private final ScheduleService scheduleService;
    private final DeadlineService deadlineService;
    private final StarostaService starostaService;
    private final ScheduleChangeService scheduleChangeService;
    private final StudentAbsenceService studentAbsenceService;
    private final NotificationPreferencesService notificationPreferencesService;

    @GetMapping
    public ResponseEntity<UserDTO> getMe(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        return ResponseEntity.ok(currentUser(authorization));
    }

    @GetMapping("/schedule")
    public ResponseEntity<List<ScheduleDTO>> getMySchedule(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        UserDTO user = currentUser(authorization);
        validateRange(from, to);
        if (user.getGroupName() == null || user.getGroupName().isBlank()) {
            return ResponseEntity.ok(List.of());
        }

        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        List<ScheduleDTO> schedule = LongStream.range(0, inclusiveDays)
                .mapToObj(from::plusDays)
                .flatMap(date -> scheduleService.getScheduleForDay(
                        user.getGroupName(),
                        date.format(DateUtils.FORMATTER)).stream())
                .toList();
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/deadlines")
    public ResponseEntity<List<MiniAppDeadlineResponse>> getMyDeadlines(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        UserDTO user = currentUser(authorization);
        if (user.getGroupName() == null || user.getGroupName().isBlank()) {
            return ResponseEntity.ok(List.of());
        }
        List<MiniAppDeadlineResponse> deadlines = deadlineService.getAllByGroup(user.getGroupName()).stream()
                .map(MiniAppDeadlineResponse::from)
                .toList();
        return ResponseEntity.ok(deadlines);
    }

    @PostMapping("/starosta")
    public ResponseEntity<UserDTO> claimStarosta(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(starostaService.claimOwnGroup(telegramId));
    }

    @DeleteMapping("/starosta")
    public ResponseEntity<UserDTO> releaseStarosta(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(starostaService.releaseOwnGroup(telegramId));
    }

    @PostMapping("/schedule/changes")
    public ResponseEntity<MiniAppScheduleChangeResponse> changeSchedule(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody MiniAppScheduleChangeRequest request
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(scheduleChangeService.apply(request, telegramId));
    }

    @GetMapping("/absences")
    public ResponseEntity<List<StudentAbsenceResponse>> getOwnAbsences(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(studentAbsenceService.getOwn(telegramId));
    }

    @PostMapping("/absences")
    public ResponseEntity<StudentAbsenceResponse> createAbsence(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody StudentAbsenceRequest request
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(studentAbsenceService.create(telegramId, request));
    }

    @DeleteMapping("/absences/{absenceId}")
    public ResponseEntity<Void> deleteOwnAbsence(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long absenceId
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        studentAbsenceService.deleteOwn(telegramId, absenceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/group/absences")
    public ResponseEntity<List<GroupStudentAbsenceResponse>> getOwnGroupAbsences(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(studentAbsenceService.getOwnGroup(telegramId));
    }

    @GetMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsResponse> getNotificationSettings(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(notificationPreferencesService.getSettings(telegramId));
    }

    @PutMapping("/notification-settings")
    public ResponseEntity<NotificationSettingsResponse> updateNotificationSettings(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody UpdateNotificationSettingsRequest request
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(notificationPreferencesService.updateSettings(telegramId, request));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationHistoryResponse>> getNotificationHistory(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(notificationPreferencesService.getHistory(telegramId));
    }

    private UserDTO currentUser(String authorization) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return userService.getUserByTelegramId(telegramId);
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new MiniAppRequestException("Дата окончания раньше даты начала");
        }
        long inclusiveDays = ChronoUnit.DAYS.between(from, to) + 1;
        if (inclusiveDays > MAX_SCHEDULE_RANGE_DAYS) {
            throw new MiniAppRequestException("Диапазон расписания не может превышать 31 день");
        }
    }
}
