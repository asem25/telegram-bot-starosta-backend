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

    Optional<UserEntity> findByTelegramId(Long telegramId);

    /**
     * Assigns the first group without allowing a concurrent request to replace it.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE users
            SET group_id = :groupId
            WHERE id = :userId
              AND group_id IS NULL
            """, nativeQuery = true)
    int assignInitialGroup(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId
    );

    /**
     * Atomically creates a pseudonymous Mini App user. Re-authentication does not
     * touch role, group or any teacher-only profile fields.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO users (telegram_id, role, group_id)
            VALUES (:telegramId, 'STUDENT', NULL)
            ON CONFLICT (telegram_id) DO NOTHING
            """, nativeQuery = true)
    void upsertTelegramUser(
            @Param("telegramId") Long telegramId
    );

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
                patronymic, role, teacher_uuid, group_id
            ) VALUES (
                :firstName, :lastName, :patronymic,
                :role, :teacherUuid, :groupId
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
            @Param("groupId") Long groupId
    );

    @Query("""
            SELECT ue from UserEntity ue where ue.teacherUuid IN :setIds
                        GROUP BY ue.teacherUuid
            """)
    Map<String, UserEntity> collectAllWithIds(@Param("setIds") Set<String> setIds);

}
