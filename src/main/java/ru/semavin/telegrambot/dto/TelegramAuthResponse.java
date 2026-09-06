package ru.semavin.telegrambot.dto;

public record TelegramAuthResponse(
        String accessToken,
        long expiresInSeconds,
        UserDTO user
) {
}
