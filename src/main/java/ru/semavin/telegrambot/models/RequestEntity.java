package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;

/**
 * Сущность для хранения заявок на вступление в группу.
 * Если статус не храните, значит при одобрении просто удаляете запись.
 */
@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestEntity {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Пользователь, который хочет вступить в группу (или на которого подают заявку).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserEntity user;

    /**
     * Группа, в которую пользователь хочет вступить.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "id", nullable = false)
    private GroupEntity group;
}
