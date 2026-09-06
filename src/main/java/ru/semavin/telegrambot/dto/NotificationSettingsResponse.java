package ru.semavin.telegrambot.dto;

public record NotificationSettingsResponse(
        boolean scheduleChangesEnabled,
        boolean deadlineRemindersEnabled,
        boolean telegramWriteAccessGranted
) {
}
