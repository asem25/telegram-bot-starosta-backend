package ru.semavin.telegrambot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
public class ScheduleDTO {
    private String groupName;
    private String subjectName;
    private String lessonType;  // ЛК, ПЗ, ЛР
    private String teacherName;
    private String classroom;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
