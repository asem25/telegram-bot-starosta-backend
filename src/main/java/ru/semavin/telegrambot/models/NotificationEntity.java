package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    /**
     * Автоинкрементный первичный ключ (ID)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Дополнительный UUID (не PK, просто уникальный идентификатор).
     * Можно заполнить в конструкторе или билдере, например UUID.randomUUID().
     */
    @Column(name = "uuid_id", nullable = false, updatable = false)
    private UUID uuid;

    /**
     * Ссылка на сущность пользователя (студента, который пропускает).
     * Хранится как внешний ключ в колонке user_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity username; // можно назвать user, чтобы было понятнее

    /**
     * Ссылка на сущность группы.
     * Хранится как внешний ключ в колонке group_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private GroupEntity groupName; // можно назвать groupEntity, чтобы было понятнее

    /**
     * Описание/причина пропуска
     */
    @Column(name = "description")
    private String description;

    /**
     * Дата начала пропуска
     */
    @Column(name = "from_date", nullable = false)
    private LocalDate fromDate;

    /**
     * Дата окончания пропуска
     */
    @Column(name = "to_date", nullable = false)
    private LocalDate toDate;
}
