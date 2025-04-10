package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.schedules.ScheduleService;
import ru.semavin.telegrambot.utils.DateUtils;

import java.util.List;

/**
 * Контроллер для получения расписания.
 */
@RestController
@RequestMapping("api/v1/schedule")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Schedule Controller", description = "Публичный контроллер для получения расписания")
public class ScheduleController {
    //TODO GET /month GET /semestr (teacher)
    private final ScheduleService scheduleService;
    private final ScheduleChangeService scheduleChangeService;
    /**
     * Получение расписания за указанную или текущую неделю.
     *
     * @param groupName Название группы (обязательный параметр).
     * @param week Номер недели (необязательный, если не указан, будет выбрана текущая неделя).
     * @return Список пар в формате {@link ScheduleDTO}.
     */
    @Operation(
            summary = "Получение расписания за неделю",
            description = "Позволяет получить расписание для указанной группы на выбранную неделю. "
                    + "Если неделя не указана, используется текущая неделя."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение расписания"),
            @ApiResponse(responseCode = "404", description = "Расписание не найдено")
    })
    @GetMapping("/week")
    public ResponseEntity<List<ScheduleDTO>> getSchedule(
            @Parameter(description = "Название группы", required = true) @RequestParam String groupName,
            @Parameter(description = "Номер недели (необязательный параметр)") @RequestParam(required = false) String week) {
        return ResponseEntity.ok(scheduleService.getScheduleFromDataBase(groupName));
    }

    /**
     * Получение расписания на текущий день.
     *
     * @param groupName Название группы.
     * @return Список пар на текущий день.
     */
    @Operation(
            summary = "Получение расписания на текущий день",
            description = "Позволяет получить расписание для указанной группы на сегодняшний день."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение расписания"),
            @ApiResponse(responseCode = "404", description = "Расписание не найдено")
    })
    @GetMapping("/currentDay")
    public ResponseEntity<List<ScheduleDTO>> getScheduleForCurrentDay(
            @Parameter(description = "Название группы", required = true) @RequestParam String groupName) {
        return ResponseEntity.ok(scheduleService.getScheduleForDay(groupName, DateUtils.getTodayWithCheckSunDay()));
    }

    /**
     * Получение расписания на определенный день.
     *
     * @param groupName Название группы.
     * @param date Дата в формате "dd.MM.yyyy".
     * @return Список пар на заданный день.
     */
    @Operation(
            summary = "Получение расписания на заданный день",
            description = "Позволяет получить расписание для указанной группы на определенную дату."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение расписания"),
            @ApiResponse(responseCode = "404", description = "Расписание не найдено")
    })
    @GetMapping("/day")
    public ResponseEntity<List<ScheduleDTO>> getScheduleForDay(
            @Parameter(description = "Название группы", required = true) @RequestParam String groupName,
            @Parameter(description = "Дата в формате 'dd.MM.yyyy'", required = true) @RequestParam String date) {
        return ResponseEntity.ok(scheduleService.getScheduleForDay(groupName, date));
    }
    /**
     * Получение расписания на завтрашний день.
     *
     * @param groupName Название группы.
     * @return Список пар на заданный день.
     */
    @Operation(
            summary = "Получение расписания на завтрашний день",
            description = "Позволяет получить расписание для указанной группы на определенную дату."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешное получение расписания"),
            @ApiResponse(responseCode = "404", description = "Расписание не найдено")
    })
    @GetMapping("/tomorrow")
    public ResponseEntity<List<ScheduleDTO>> getScheduleForTomorrow(
            @Parameter(description = "Название группы", required = true) @RequestParam String groupName) {
        return ResponseEntity.ok(scheduleService.getScheduleForDay(groupName, DateUtils.getTomorrowWithCheckSunDay()));
    }

    /**
     * Удаление одной пары по её ID.
     */
    @DeleteMapping("/change")
    @Operation(summary = "Отметить удаление пары в таблице изменений")
    public ResponseEntity<String> markScheduleAsDeleted(
            @RequestBody ScheduleChangeDTO changeDto,
            @RequestParam String groupName
    ) {
        scheduleChangeService.markAsDeleted(changeDto, groupName);
        return ResponseEntity.ok("Удаление пары отмечено в изменениях расписания");
    }

    @GetMapping("/lesson")
    @Operation(summary = "Найти пару")
    public ResponseEntity<ScheduleDTO> getLessonByGroupNameAndDate(
            @RequestParam String groupName,
            @RequestParam String date,
            @RequestParam String startTime
    ) {

        return ResponseEntity.ok(scheduleService.findLesson(groupName, date, startTime));
    }

    /**
     * Редактирование (обновление) одной пары по её ID.
     */
    @PutMapping("/change")
    @Operation(summary = "Редактировать расписание с сохранением изменений в отдельной таблице")
    public ResponseEntity<String> editSchedule(
            @RequestBody ScheduleChangeDTO changeDto,
            @RequestParam String groupName
    ) {
        scheduleChangeService.createOrUpdate(changeDto, groupName);
        return ResponseEntity.ok("Изменение расписания сохранено");
    }

}
