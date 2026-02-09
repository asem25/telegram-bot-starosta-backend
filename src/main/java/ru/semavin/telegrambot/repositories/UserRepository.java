package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.UserEntity;

import java.util.Optional;

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

}
