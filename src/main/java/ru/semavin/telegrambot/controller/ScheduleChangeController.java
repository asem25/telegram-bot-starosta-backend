package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.dto.ScheduleChangeForEveryDayCheckDTO;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.schedules.SemesterService;

import java.time.LocalDate;

@RestController
@RequestMapping("api/v1/schedule")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schedule Change API")
public class ScheduleChangeController {
    //TODO добавить получение и удаление измене
    private final ScheduleChangeService changeService;
    private final SemesterService semesterService;

    /**
     * Удаление одной пары по её ID.
     */
    @DeleteMapping("/change")
    @Operation(summary = "Отметить удаление пары в таблице изменений")
    public ResponseEntity<String> markScheduleAsDeleted(
            @RequestBody ScheduleChangeDTO changeDto,
            @RequestParam String groupName
    ) {
        changeService.markAsDeleted(changeDto, groupName);
        return ResponseEntity.ok("Удаление пары отмечено в изменениях расписания");
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
        changeService.createOrUpdate(changeDto, groupName);
        return ResponseEntity.ok("Изменение расписания сохранено");
    }

    @GetMapping(value = "/change", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ScheduleChangeForEveryDayCheckDTO> getChangeForDate(
            @RequestParam String groupName,
            @RequestParam String date
    ) {
        LocalDate parsingDate = semesterService.getFormatterDate(date);

        log.info("Отправке изменений расписания группе {} на дату {}", groupName, date);
        return ResponseEntity.ok(changeService
                .getChangesDtoForDay(groupName, parsingDate));
    }
}
