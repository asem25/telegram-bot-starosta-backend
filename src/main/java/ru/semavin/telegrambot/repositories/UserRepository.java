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

    @Query(value = """
            INSERT INTO users (
                first_name,
                last_name,
                patronymic,
                role,
                teacher_uuid,
                telegram_id,
                username,
                group_id
            ) VALUES (
                :firstName,
                :lastName,
                :patronymic,
                :role,
                :teacherUuid,
                :teacherId,
                :username,
                :groupId
            )
            ON CONFLICT (teacher_uuid)
            DO UPDATE
            SET teacher_uuid = EXCLUDED.teacher_uuid
            RETURNING id;
            """, nativeQuery = true)
    Long insertWithConflict(
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
