package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;

@RequestMapping("api/v1/users")
@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Controller", description = "Контроллер для пользователей ботом")
public class UserController {
    private final UserService userService;
    @Value("${key.api}")
    private String keyApi;
    @PostMapping("")
    @Operation(summary = "Добавление пользователя")
    @SecurityRequirement(name = "API-KEY")
    public ResponseEntity<String> addUser(@RequestBody UserDTO user,
                                          @RequestHeader("API-KEY") String key) {
        if (!keyApi.equals(key)) {
            log.warn("API key in header not equals");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Adding user: {}", user);
        String userNameSaveUser = userService.save(user);
        log.info("User added: {}", userNameSaveUser);
        return ResponseEntity.ok(userNameSaveUser);
    }
}
