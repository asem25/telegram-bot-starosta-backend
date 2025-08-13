package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.DeadlineDTO;
import ru.semavin.telegrambot.services.deadline.DeadlineService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/deadlines")
@RequiredArgsConstructor
@Tag(name = "Deadlines", description = "Работа с дедлайнами")
@Slf4j
public class DeadlineController {
    private final DeadlineService deadlineService;

    @Operation(summary = "Получить все дедлайны группы")
    @GetMapping("/group/{groupName}")
    public ResponseEntity<List<DeadlineDTO>> getAllByGroup(@PathVariable String groupName) {
        log.info("GET api/v1/deadlines/group/, groupName {}", groupName);
        return ResponseEntity.ok(deadlineService.getAllByGroup(groupName));
    }

    @Operation(summary = "Создание нового дедлайна")
    @PostMapping
    public ResponseEntity<DeadlineDTO> createDeadline(@RequestBody DeadlineDTO dto) {
        log.info("POST api/v1/deadlines, body {}", dto);
        return ResponseEntity.ok(deadlineService.save(dto));
    }

    @Operation(summary = "Удалить дедлайн")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeadline(@PathVariable String id) {
        log.info("DELETE /api/v1/deadlines, id {}", id);
        deadlineService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Получить дедлайны в указанном диапазоне дат")
    @GetMapping("/reminders")
    public ResponseEntity<List<DeadlineDTO>> getDeadlinesInPeriod(
            @RequestParam("from") LocalDate from,
            @RequestParam("to") LocalDate to
    ) {
        log.info("GET /api/v1/reminders, from {} to {}", from, to);
        return ResponseEntity.ok(deadlineService.getDeadlinesBetween(from, to));
    }

    @Operation(summary = "Отметить дедлайн как уведомлённый")
    @PatchMapping("/{id}/notify")
    public ResponseEntity<Void> markNotified(@PathVariable String id, @RequestBody Map<String, Boolean> request) {
        log.info("POST /api/v1/reminders, id {}", id);
        boolean notified3 = request.getOrDefault("notified3Days", false);
        boolean notified1 = request.getOrDefault("notified1Day", false);
        deadlineService.markNotified(id, notified3, notified1);
        return ResponseEntity.noContent().build();
    }
}
