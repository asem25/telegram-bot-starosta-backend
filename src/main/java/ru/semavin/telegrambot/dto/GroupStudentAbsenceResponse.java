package ru.semavin.telegrambot.dto;

import ru.semavin.telegrambot.models.StudentAbsenceEntity;
import ru.semavin.telegrambot.models.UserEntity;

import java.time.LocalDate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public record GroupStudentAbsenceResponse(
        Long id,
        Long studentId,
        String studentName,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
    public static GroupStudentAbsenceResponse from(StudentAbsenceEntity absence) {
        UserEntity student = absence.getUser();
        String name = Stream.of(student.getFirstName(), student.getLastName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(" "));
        if (name.isBlank()) {
            name = student.getUsername() == null || student.getUsername().isBlank()
                    ? "Студент"
                    : "@" + student.getUsername();
        }
        return new GroupStudentAbsenceResponse(
                absence.getId(),
                student.getId(),
                name,
                absence.getStartDate(),
                absence.getEndDate(),
                absence.getReason());
    }
}
