package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.MiniAppGroupRequest;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.auth.AccessTokenService;

@RestController
@RequestMapping("api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Controller", description = "Контроллер для управления пользователями")
public class UserController {

    private final UserService userService;
    private final AccessTokenService accessTokenService;

    @PatchMapping("/me")
    @Operation(
            summary = "Выбор своей учебной группы",
            description = "Однократно назначает учебную группу пользователю Telegram Mini App.",
            security = @SecurityRequirement(name = "Bearer Authentication"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Группа успешно выбрана"),
                    @ApiResponse(responseCode = "400", description = "Некорректный формат группы"),
                    @ApiResponse(responseCode = "401", description = "Telegram-токен невалиден"),
                    @ApiResponse(responseCode = "404", description = "Пользователь или группа не найдены"),
                    @ApiResponse(responseCode = "409", description = "Группа уже выбрана")
            }
    )
    public ResponseEntity<UserDTO> selectOwnGroup(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestBody @Valid MiniAppGroupRequest request
    ) {
        long telegramId = accessTokenService.requireTelegramId(authorization);
        return ResponseEntity.ok(userService.assignInitialGroup(telegramId, request.groupName()));
    }
}
