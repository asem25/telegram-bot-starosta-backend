package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;
import ru.semavin.telegrambot.models.enums.LessonType;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="group_name")
    private String groupName;

    @Column(name="subject_name")
    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(name="lesson_type")
    private LessonType lessonType;  // ЛК, ПЗ, ЛР

    @Column(name="teacher_name")
    private String teacherName;

    @Column(name="classroom")
    private String classroom;

    @Column(name="lesson_date")
    private LocalDate lessonDate;

    @Column(name="start_time")
    private LocalTime startTime;

    @Column(name="end_time")
    private LocalTime endTime;
    @Column(name="lesson_week")
    private Integer lessonWeek;
}
