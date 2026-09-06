package ru.semavin.telegrambot.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID id,
        String type,
        String title,
        String body,
        OffsetDateTime createdAt,
        String telegramDeliveryStatus
) {
}
