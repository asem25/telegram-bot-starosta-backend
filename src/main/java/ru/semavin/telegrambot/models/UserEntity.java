package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;
import ru.semavin.telegrambot.models.enums.UserRole;

import java.util.HashSet;
import java.util.Set;

/**
 * Сущность, описывающая пользователя системы (студента, старосту, преподавателя и т.д.).
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class UserEntity {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный идентификатор пользователя в Telegram.
     */
    @Column(name = "telegram_id", unique = true)
    private Long telegramId;

    /**
     * Никнейм пользователя (username) в Telegram.
     */
    @Column(name = "username")
    private String username;
    /**
     * Отчество пользователя
     */
    @Column(name = "patronymic")
    private String patronymic;
    /**
     * Имя пользователя.
     */
    @Column(name = "first_name")
    private String firstName;

    /**
     * Фамилия пользователя.
     */
    @Column(name = "last_name")
    private String lastName;

    /**
     * Роль пользователя в системе (например, STUDENT, STAROSTA, ADMIN, TEACHER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @ToString.Include
    private UserRole role;

    /**
     * Ссылка на группу, к которой принадлежит пользователь (для студентов).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "id", nullable = true)
    private GroupEntity group;

    /**
     * Уникальный идентификатор преподавателя (teacher UUID) из парсинга расписания.
     * Заполняется только для пользователей с ролью TEACHER.
     */
    @Column(name = "teacher_uuid", unique = true)
    private String teacherUuid;

    @Column(name = "schedule_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean scheduleNotificationsEnabled = true;

    @Column(name = "deadline_notifications_enabled", nullable = false)
    @Builder.Default
    private boolean deadlineNotificationsEnabled = true;

    @Column(name = "telegram_write_access_granted", nullable = false)
    private boolean telegramWriteAccessGranted;

    /**
     * Список групп, в которых преподаёт данный пользователь (при роли TEACHER).
     */
    @ManyToMany
    @JoinTable(
            name = "teacher_groups",
            joinColumns = @JoinColumn(name = "teacher_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    @Builder.Default
    private Set<GroupEntity> teachingGroups = new HashSet<>();

}
