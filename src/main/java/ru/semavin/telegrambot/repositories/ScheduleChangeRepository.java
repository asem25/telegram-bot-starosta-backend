package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleChangeRepository extends JpaRepository<ScheduleChangeEntity, Long> {

    List<ScheduleChangeEntity> findAllByGroupAndOldLessonDate(GroupEntity group, LocalDate date);

    @Query("""
        SELECT sc from ScheduleChangeEntity sc
                where sc.group = :group
                        AND (sc.newLessonDate = :date
                                OR sc.oldLessonDate = :date)
        """)
    List<ScheduleChangeEntity> findAllByGroupAndDate(@Param("group") GroupEntity group,
                                                     @Param("date") LocalDate date);

}

