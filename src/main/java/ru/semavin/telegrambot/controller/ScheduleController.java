package ru.semavin.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.services.ScheduleService;

import java.util.List;

@RestController
@RequestMapping("api/v1/schedule")
@Slf4j
@RequiredArgsConstructor
public class ScheduleController {
    private final ScheduleService scheduleService;

    @GetMapping()
    public ResponseEntity<List<ScheduleDTO>> getSchedule(@RequestParam String groupName,
                                                         @RequestParam(required = false) String week){
        return ResponseEntity.ok(scheduleService.getScheduleFromDataBase(groupName, week));
    }
}
