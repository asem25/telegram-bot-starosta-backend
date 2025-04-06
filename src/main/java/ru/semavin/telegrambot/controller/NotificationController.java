package ru.semavin.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.NotificationDTO;
import ru.semavin.telegrambot.services.NotificationService;

import java.util.List;

@RestController
@RequestMapping("api/v1/notification")
@Slf4j
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PostMapping("/add")
    public ResponseEntity<String> addNotification(@RequestBody NotificationDTO notificationDTO) {
        log.info("POST notification/add");
        notificationService.add(notificationDTO);
        return ResponseEntity.status(204).body("Successful");
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteNotification(@RequestParam String id) {
        log.info("DELETE notification/delete");
        notificationService.deleteByUUID(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(@RequestParam String groupName) {
        log.info("GET notification");
        return ResponseEntity.ok(notificationService.getAll(groupName));
    }
}
