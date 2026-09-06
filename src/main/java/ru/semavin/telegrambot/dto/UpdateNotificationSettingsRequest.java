package ru.semavin.telegrambot.dto;

public record UpdateNotificationSettingsRequest(
        boolean scheduleChangesEnabled,
        boolean deadlineRemindersEnabled,
        boolean telegramWriteAccessGranted
) {
}
