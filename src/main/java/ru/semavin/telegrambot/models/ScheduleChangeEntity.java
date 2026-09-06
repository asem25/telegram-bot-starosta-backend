package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "schedule_changes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_schedule_change_group_request", columnNames = {"group_id", "client_request_id"}),
        @UniqueConstraint(name = "uk_schedule_change_occurrence_version", columnNames = {"occurrence_id", "change_version"}),
        @UniqueConstraint(name = "uk_schedule_change_batch_occurrence", columnNames = {"group_id", "batch_request_id", "occurrence_id"})
})
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ScheduleChangeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurrence_id")
    private UUID occurrenceId;

    @Column(name = "series_id")
    private UUID seriesId;

    @Column(name = "client_request_id")
    private UUID clientRequestId;

    @Column(name = "batch_request_id")
    private UUID batchRequestId;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Column(name = "operation", length = 16)
    private String operation;

    @Column(name = "change_version")
    private Long version;

    @Column(name = "created_at")
    private Instant createdAt;

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

    private String oldControlSum;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private GroupEntity group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private UserEntity author;

    @PrePersist
    void setCreationTime() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
