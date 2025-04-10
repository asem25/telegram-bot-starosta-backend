package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "schedule_changes")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleChangeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity group;
}

