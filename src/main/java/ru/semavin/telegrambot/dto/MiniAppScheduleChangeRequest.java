package ru.semavin.telegrambot.dto;

import jakarta.validation.constraints.NotNull;
import ru.semavin.telegrambot.models.enums.ScheduleChangeOperation;
import ru.semavin.telegrambot.models.enums.ScheduleChangeScope;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record MiniAppScheduleChangeRequest(
        @NotNull UUID lessonOccurrenceId,
        @NotNull UUID clientRequestId,
        @NotNull Long expectedVersion,
        @NotNull ScheduleChangeOperation operation,
        @NotNull ScheduleChangeScope scope,
        String subjectName,
        String lessonType,
        String teacherName,
        String classroom,
        String description,
        LocalDate newLessonDate,
        LocalTime newStartTime,
        LocalTime newEndTime
) {
}
