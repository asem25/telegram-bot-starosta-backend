package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.services.ScheduleChangeService;

@RestController
@RequestMapping("/api/schedule-changes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Schedule Change API")
public class ScheduleChangeController {
    //TODO добавить получение и удаление измене
    private final ScheduleChangeService changeService;

    @PostMapping
    @Operation(summary = "Создание или обновление изменений занятия")
    public ResponseEntity<String> createOrUpdate(@RequestBody @Valid ScheduleChangeDTO dto,
                                                 @RequestParam String groupName) {
        changeService.createOrUpdate(dto, groupName);
        return ResponseEntity.ok("Изменение сохранено");
    }
}
