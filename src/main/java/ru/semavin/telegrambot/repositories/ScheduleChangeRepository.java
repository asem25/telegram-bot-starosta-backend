package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleChangeRepository extends JpaRepository<ScheduleChangeEntity, Long> {

    Optional<ScheduleChangeEntity> findByGroupAndClientRequestId(GroupEntity group, UUID clientRequestId);

    List<ScheduleChangeEntity> findAllByGroupAndBatchRequestIdOrderByIdAsc(
            GroupEntity group,
            UUID batchRequestId
    );

    Optional<ScheduleChangeEntity> findFirstByOccurrenceIdOrderByVersionDesc(UUID occurrenceId);

    List<ScheduleChangeEntity> findAllByOccurrenceIdOrderByVersionAsc(UUID occurrenceId);

    List<ScheduleChangeEntity> findAllByGroupOrderByIdAsc(GroupEntity group);

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
