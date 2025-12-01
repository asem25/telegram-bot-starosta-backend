package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO для расписания.
 */
@Data
@Builder
@AllArgsConstructor
@Schema(description = "DTO для расписания")
public class ScheduleDTO {
    private Long id;
    @Schema(description = "Название группы", example = "М3О-303С-22")
    private String groupName;

    @Schema(description = "Название предмета", example = "ОТУ")
    private String subjectName;

    @Schema(description = "Тип занятия (ЛК, ПЗ, ЛР)", example = "ЛК")
    private String lessonType; // ЛК, ПЗ, ЛР

    @Schema(description = "Имя преподавателя", example = "Иванов Иван Иванович")
    private String teacherName;

    @Schema(description = "Аудитория", example = "Ауд. 301")
    private String classroom;

    @Schema(description = "Описание(если есть какие то изменения)", example = "Перенос на 13.00")
    private String description;

    @Schema(description = "Дата занятия", example = "2025-03-20")
    private LocalDate lessonDate;

    @Schema(description = "Время начала занятия", example = "10:00")
    private LocalTime startTime;

    @Schema(description = "Контрольная сумма занятия")
    private String controlSum;

    @Schema(description = "Время окончания занятия", example = "11:30")
    private LocalTime endTime;
}
