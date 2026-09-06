package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import ru.semavin.telegrambot.models.GroupEntity;

import java.util.Optional;

public interface GroupRepository extends JpaRepository<GroupEntity, Long> {
    Optional<GroupEntity> findByGroupNameIgnoreCase(String groupName);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GroupEntity g left join fetch g.starosta where g.id = :id")
    Optional<GroupEntity> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE groups AS g
            SET starosta_id = u.id
            FROM users AS u
            WHERE u.telegram_id = :telegramId
              AND u.group_id = g.id
              AND g.starosta_id IS NULL
            """, nativeQuery = true)
    int claimStarostaForOwnGroup(@Param("telegramId") Long telegramId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE groups AS g
            SET starosta_id = NULL
            FROM users AS u
            WHERE u.telegram_id = :telegramId
              AND u.group_id = g.id
              AND g.starosta_id = u.id
            """, nativeQuery = true)
    int releaseStarostaForOwnGroup(@Param("telegramId") Long telegramId);

}
