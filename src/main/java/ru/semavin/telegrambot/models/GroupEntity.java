package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Сущность, описывающая учебную группу (или сообщество).
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupEntity {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Название (код) группы, например "М3О-303С-22".
     */
    @Column(name = "group_name", nullable = false, length = 20)
    private String groupName;

    /**
     * Ссылка на пользователя, который является старостой группы.
     * Если не нужно, можно убрать вместе с соответствующим столбцом в БД.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "starosta_id", referencedColumnName = "id", nullable = true)
    private UserEntity starosta;

    /**
     * Список пользователей, относящихся к этой группе.
     * "mappedBy = group" означает, что связь описана в UserEntity.group.
     */
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserEntity> users = new ArrayList<>();
}
