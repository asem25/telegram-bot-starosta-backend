package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.enums.LessonType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {

    List<ScheduleEntity> findAllByLessonDateAndGroup(LocalDate lessonDate, GroupEntity group);

    /**
     * Удаляет все записи расписания для указанной группы
     *
     * @param group название групп
     */

    void deleteAllByGroup(GroupEntity group);

    ScheduleEntity findByGroupAndLessonDateAndStartTime(GroupEntity group, LocalDate lessonDate, LocalTime startTime);

    @Query("""
            select sh from ScheduleEntity sh
                        where sh.group = :group
                        and sh.lessonDate = :lessonDate
                        and sh.startTime = :startTime
                        and sh.endTime = :endTime
                        and sh.lessonType = :type
                        and sh.subjectName = :subName
            """)
    ScheduleEntity findSchedule(@Param("group") GroupEntity group,
                                @Param("lessonDate") LocalDate lessonDate,
                                @Param("startTime") LocalTime startTimeParse,
                                @Param("endTime") LocalTime endTimeParse,
                                @Param("type") LessonType lessonTypeParse,
                                @Param("subName") String subjectName);
}
