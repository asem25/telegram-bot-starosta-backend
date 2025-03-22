package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;
import ru.semavin.telegrambot.models.enums.UserRole;

/**
 * Сущность, описывающая пользователя системы (студента, старосту и т.д.).
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

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
     * Имя пользователя
     */
    @Column(name = "first_name")
    private String firstName;
    /**
     * Фамилия пользователя
     */
    @Column(name = "last_name")
    private String lastName;
    /**
     * Роль пользователя в системе (например, STUDENT, STAROSTA, ADMIN).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role;

    /**
     * Ссылка на группу, к которой принадлежит пользователь.
     * В БД это поле users.group_id → groups.id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "id", nullable = true)
    private GroupEntity group;
}
