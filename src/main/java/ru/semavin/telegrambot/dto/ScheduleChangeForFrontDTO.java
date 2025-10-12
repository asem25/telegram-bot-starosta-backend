package ru.semavin.telegrambot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleChangeForFrontDTO {
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

    private String description;
    private boolean deleted;
}
