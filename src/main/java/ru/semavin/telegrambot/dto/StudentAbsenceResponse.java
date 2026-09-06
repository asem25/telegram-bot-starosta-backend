package ru.semavin.telegrambot.dto;

import ru.semavin.telegrambot.models.StudentAbsenceEntity;

import java.time.LocalDate;

public record StudentAbsenceResponse(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
    public static StudentAbsenceResponse from(StudentAbsenceEntity absence) {
        return new StudentAbsenceResponse(
                absence.getId(),
                absence.getStartDate(),
                absence.getEndDate(),
                absence.getReason());
    }
}
