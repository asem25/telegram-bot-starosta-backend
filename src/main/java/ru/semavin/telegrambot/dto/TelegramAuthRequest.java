package ru.semavin.telegrambot.dto;

import jakarta.validation.constraints.NotBlank;

public record TelegramAuthRequest(
        @NotBlank(message = "Telegram initData не может быть пустым")
        String initData
) {
}
