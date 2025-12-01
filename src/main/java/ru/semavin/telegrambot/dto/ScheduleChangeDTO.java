package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "DTO для изменений в расписании")
public class ScheduleChangeDTO {
    private String subjectName;
    private String lessonType;
    private String teacherName;
    private String classroom;
    private LocalDate oldLessonDate;
    private LocalTime oldStartTime;
    private LocalTime oldEndTime;

    private LocalDate newLessonDate;
    private LocalTime newStartTime;
    private LocalTime newEndTime;

    private String controlSum;

    private String description;
}
