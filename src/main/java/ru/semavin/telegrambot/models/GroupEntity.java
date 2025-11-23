package ru.semavin.telegrambot.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Сущность, описывающая учебную группу (или сообщество).
 */
@Entity
@Table(name = "groups")
@Data
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
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "starosta_id", referencedColumnName = "id", nullable = true)
    private UserEntity starosta;

    /**
     * Список пользователей, относящихся к этой группе.
     */
    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserEntity> users = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupEntity group = (GroupEntity) o;
        return Objects.equals(groupName, group.groupName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName);
    }

    @Override
    public String toString() {
        return "GroupEntity{" +
                "id=" + id +
                ", groupName='" + groupName + '\'' +
                ", starosta=" + (
                        starosta == null ?
                "Не назначен" :
                starosta.getId().toString())
                + '}';
    }
}
