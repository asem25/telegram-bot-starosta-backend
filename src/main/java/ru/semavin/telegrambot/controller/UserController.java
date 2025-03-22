package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.KeyNotEqualsException;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Controller", description = "Контроллер для управления пользователями")
public class UserController {

    private final UserService userService;

    @Value("${key.api}")
    private String keyApi;

    @PostMapping("")
    @Operation(
            summary = "Добавление пользователя",
            description = "Создаёт нового пользователя в системе.",
            security = @SecurityRequirement(name = "API-KEY"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно добавлен"),
                    @ApiResponse(responseCode = "400", description = "Некорректные данные при валидации"),
                    @ApiResponse(responseCode = "401", description = "Неверный API-KEY"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            }
    )
    public ResponseEntity<String> addUser(
            @RequestBody @Valid UserDTO user,
            @Parameter(description = "API-KEY для авторизации", required = true)
            @RequestHeader("API-KEY") String key
    ) {
        if (!keyApi.equals(key)) {
            log.warn("API key in header not equals");
            throw ExceptionFabric.create(KeyNotEqualsException.class, ExceptionMessages.KEY_NOT_VALID);
        }
        log.info("Adding user: {}", user);
        String userNameSaveUser = userService.save(user);
        log.info("User added: {}", userNameSaveUser);
        return ResponseEntity.ok(userNameSaveUser);
    }

    @PatchMapping("")
    @Operation(
            summary = "Обновление пользователя",
            description = "Обновляет информацию о пользователе в системе.",
            security = @SecurityRequirement(name = "API-KEY"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлён"),
                    @ApiResponse(responseCode = "400", description = "Некорректные данные при валидации"),
                    @ApiResponse(responseCode = "401", description = "Неверный API-KEY"),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            }
    )
    public ResponseEntity<String> updateUser(
            @RequestBody @Valid UserDTO user,
            @Parameter(description = "API-KEY для авторизации", required = true)
            @RequestHeader("API-KEY") String key
    ) {
        if (!keyApi.equals(key)) {
            log.warn("API key in header not equals");
            throw ExceptionFabric.create(KeyNotEqualsException.class, ExceptionMessages.KEY_NOT_VALID);
        }
        log.info("Updating user: {}", user);
        String userNameSaveUser = userService.update(user);
        log.info("User updated: {}", userNameSaveUser);
        return ResponseEntity.ok(userNameSaveUser);
    }
}
