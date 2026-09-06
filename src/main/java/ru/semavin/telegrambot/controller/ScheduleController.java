package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.semavin.telegrambot.services.schedules.SchedulerCalendarISCService;

/**
 * Контроллер для получения расписания.
 */
@RestController
@RequestMapping("api/v1/schedule")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Schedule Controller", description = "Публичный контроллер для получения расписания")
public class ScheduleController {
    private final SchedulerCalendarISCService schedulerCalendarISCService;

    @GetMapping(
            value = "/semester/feed",
            produces = "text/calendar; charset=UTF-8"
    )
    public ResponseEntity<String> getSemesterScheduleFeed(
            @Parameter(description = "Название группы", required = true)
            @RequestParam String groupName
    ) {
        log.info("Пришел запрос на получение календаря для группы {}", groupName);
        String ics = schedulerCalendarISCService.getIscCalendarByGroupName(groupName);

        log.info("Запрос на получение календаря для группы [{}] успешно отработан", groupName);
        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ics);
    }

    @GetMapping(
            value = "/teacher/semester/feed",
            produces = "text/calendar; charset=UTF-8"
    )
    public ResponseEntity<String> getTeacherSemesterScheduleFeed(
            @Parameter(description = "UUID преподавателя", required = true)
            @RequestParam String teacherUUID
    ) {

        log.info("Пришел запрос на получение календаря для преподавателя {}", teacherUUID);
        String ics = schedulerCalendarISCService.getIscCalendarByTeacher(teacherUUID);
        log.info("Получен календарь преподавателя [{}]", teacherUUID);

        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=UTF-8"))
                .body(ics);
    }

}
