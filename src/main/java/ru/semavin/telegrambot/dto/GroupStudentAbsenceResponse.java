package ru.semavin.telegrambot.dto;

import ru.semavin.telegrambot.models.StudentAbsenceEntity;
import ru.semavin.telegrambot.models.UserEntity;

import java.time.LocalDate;
public record GroupStudentAbsenceResponse(
        Long id,
        Long studentId,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
    public static GroupStudentAbsenceResponse from(StudentAbsenceEntity absence) {
        UserEntity student = absence.getUser();
        return new GroupStudentAbsenceResponse(
                absence.getId(),
                student.getId(),
                absence.getStartDate(),
                absence.getEndDate(),
                absence.getReason());
    }
}
