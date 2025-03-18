package ru.semavin.telegrambot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;

@RequestMapping("api/v1/users")
@RestController
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    @PostMapping("")
    public ResponseEntity<String> addUser(@RequestBody UserDTO user) {
        log.info("Adding user: {}", user);
        String userNameSaveUser = userService.save(user);
        log.info("User added: {}", userNameSaveUser);
        return ResponseEntity.ok(userNameSaveUser);
    }
}
