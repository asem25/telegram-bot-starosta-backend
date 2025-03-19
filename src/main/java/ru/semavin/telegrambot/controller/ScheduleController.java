package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.services.schedules.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("api/v1/schedule")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Schedule Controller", description = "Публичный контроллер для полуения расписания")
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping("/week")
    @Operation(summary = "Получения расписания за текущую неделю/выбранную")
    public ResponseEntity<List<ScheduleDTO>> getSchedule(@RequestParam String groupName,
                                                         @RequestParam(required = false) String week){
        return ResponseEntity.ok(scheduleService.getScheduleFromDataBase(groupName, week));
    }
    @GetMapping("/currentDay")
    @Operation(summary = "Получения расписания на текущий день")
    public ResponseEntity<List<ScheduleDTO>> getScheduleForCurrentDay(@RequestParam String groupName){
        return ResponseEntity.ok(scheduleService.getScheduleForCurrentDay(groupName));
    }
}
