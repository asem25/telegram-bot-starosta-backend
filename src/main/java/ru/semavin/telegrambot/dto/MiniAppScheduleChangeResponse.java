package ru.semavin.telegrambot.dto;

import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.enums.ScheduleChangeOperation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record MiniAppScheduleChangeResponse(
        UUID lessonOccurrenceId,
        UUID clientRequestId,
        long version,
        ScheduleChangeOperation operation,
        LocalDate lessonDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean cancelled,
        Instant createdAt
) {
    public static MiniAppScheduleChangeResponse from(ScheduleChangeEntity entity) {
        return new MiniAppScheduleChangeResponse(
                entity.getOccurrenceId(),
                entity.getBatchRequestId() == null
                        ? entity.getClientRequestId()
                        : entity.getBatchRequestId(),
                entity.getVersion(),
                ScheduleChangeOperation.valueOf(entity.getOperation()),
                entity.getNewLessonDate(),
                entity.getNewStartTime(),
                entity.getNewEndTime(),
                entity.isDeleted(),
                entity.getCreatedAt()
        );
    }
}
