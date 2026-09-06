package ru.semavin.telegrambot.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Public deadline representation for the browser Mini App.
 * Internal delivery state and group member identifiers must not cross this boundary.
 */
public record MiniAppDeadlineResponse(
        UUID uuid,
        String title,
        String description,
        LocalDate dueDate
) {
    public static MiniAppDeadlineResponse from(DeadlineDTO deadline) {
        return new MiniAppDeadlineResponse(
                deadline.getUuid(),
                deadline.getTitle(),
                deadline.getDescription(),
                deadline.getDueDate());
    }
}
