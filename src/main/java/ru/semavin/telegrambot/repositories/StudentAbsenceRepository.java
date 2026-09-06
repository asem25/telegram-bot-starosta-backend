package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.StudentAbsenceEntity;

import java.util.List;

public interface StudentAbsenceRepository extends JpaRepository<StudentAbsenceEntity, Long> {
    List<StudentAbsenceEntity> findAllByUserIdOrderByStartDateDesc(Long userId);

    List<StudentAbsenceEntity> findAllByUserGroupIdOrderByStartDateDesc(Long groupId);

    @Modifying
    @Query("""
            DELETE FROM StudentAbsenceEntity absence
            WHERE absence.id = :absenceId
              AND absence.user.id = :userId
            """)
    int deleteOwned(
            @Param("absenceId") Long absenceId,
            @Param("userId") Long userId
    );
}
