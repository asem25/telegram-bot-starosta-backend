package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.semavin.telegrambot.models.enums.LessonType;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Сущность расписания (schedule).
 * Хранит информацию о парах: предмет, тип, время, преподаватель и т.д.
 */
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

    /**
     * Ссылка на группу, к которой относится данное расписание.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private GroupEntity group;

    @Column(name = "subject_name")
    private String subjectName;

    @Enumerated(EnumType.STRING)
    @Column(name = "lesson_type")
    private LessonType lessonType;  // ЛК, ПЗ, ЛР (Lecture, Practical, Lab)

    /**
     * Ссылка на преподавателя (UserEntity с ролью TEACHER).
     * Для старых записей ранее использовавшихся поле teacher_name, потребуется миграция.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", referencedColumnName = "id")
    private UserEntity teacher;

    @Column(name = "classroom")
    private String classroom;

    @Column(name = "lesson_date")
    private LocalDate lessonDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "lesson_week")
    private Integer lessonWeek;

    @Column(name = "control_sum")
    private String controlSum;

    @Override
    public String toString() {
        return "ScheduleEntity{" +
                "id=" + id +
                ", group=" + group.getGroupName() +
                ", subjectName='" + subjectName + '\'' +
                ", lessonType=" + lessonType +
                ", teacher=" + teacher.getTeacherUuid() +
                ", classroom='" + classroom + '\'' +
                ", lessonDate=" + lessonDate +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", lessonWeek=" + lessonWeek +
                '}';
    }
}
