package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.UserEntity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    Optional<UserEntity> findByTelegramId(Long telegramId);

    /**
     * Assigns the first group without allowing a concurrent request to replace it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET group_id = :groupId
            WHERE telegram_id = :telegramId
              AND group_id IS NULL
            """, nativeQuery = true)
    int assignInitialGroup(
            @Param("telegramId") Long telegramId,
            @Param("groupId") Long groupId
    );

    /**
     * Atomically creates a Mini App user or refreshes the Telegram profile of an
     * existing user. The conflict branch deliberately does not touch role or
     * group_id, so opening the Mini App cannot revoke previously assigned access.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO users (telegram_id, username, first_name, last_name, role, group_id)
            VALUES (:telegramId, :username, :firstName, :lastName, 'STUDENT', NULL)
            ON CONFLICT (telegram_id) DO UPDATE SET
                username = EXCLUDED.username,
                first_name = EXCLUDED.first_name,
                last_name = EXCLUDED.last_name
            """, nativeQuery = true)
    void upsertTelegramUser(
            @Param("telegramId") Long telegramId,
            @Param("username") String username,
            @Param("firstName") String firstName,
            @Param("lastName") String lastName
    );

    boolean existsByTelegramId(Long telegramId);

    Optional<UserEntity> findByTeacherUuid(String teacherUuid);

    @Modifying
    @Query(value = """
            INSERT INTO teacher_groups (teacher_id, group_id)
            VALUES (:teacherId, :groupId)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    void insertIgnore(
            @Param("teacherId") long teacherId,
            @Param("groupId") long groupId
    );

    @Modifying
    @Query(value = """
            INSERT INTO users (first_name, last_name,
                patronymic, role, teacher_uuid,
                telegram_id, username, group_id
            ) VALUES (
                :firstName, :lastName, :patronymic,
                :role, :teacherUuid, :teacherId,
                :username, :groupId
            )
            ON CONFLICT (teacher_uuid)
            DO NOTHING;
            """, nativeQuery = true)
    void insertWithConflict(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("patronymic") String patronymic,
            @Param("role") String role,
            @Param("teacherUuid") String teacherUuid,
            @Param("teacherId") Long teacherId,
            @Param("username") String username,
            @Param("groupId") Long groupId
    );

    @Query("""
            SELECT ue from UserEntity ue where ue.teacherUuid IN :setIds
                        GROUP BY ue.teacherUuid
            """)
    Map<String, UserEntity> collectAllWithIds(@Param("setIds") Set<String> setIds);

}
