package ru.semavin.telegrambot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.semavin.telegrambot.dto.TelegramAuthRequest;
import ru.semavin.telegrambot.dto.TelegramAuthResponse;
import ru.semavin.telegrambot.services.auth.TelegramAuthService;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
public class TelegramAuthController {
    private final TelegramAuthService telegramAuthService;

    @PostMapping("/telegram")
    public ResponseEntity<TelegramAuthResponse> authenticate(
            @RequestBody @Valid TelegramAuthRequest request
    ) {
        return ResponseEntity.ok(telegramAuthService.authenticate(request.initData()));
    }
}
