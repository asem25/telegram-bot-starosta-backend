package ru.semavin.telegrambot.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class HeartBeatController {
    @GetMapping("/check")
    public ResponseEntity<String> heartbeat() {
        log.debug("Checking heartbeat at: {}", LocalDate.now());
        return ResponseEntity.ok("API is alive");
    }
}
