package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleChangeRepository extends JpaRepository<ScheduleChangeEntity, Long> {
    Optional<ScheduleChangeEntity> findByGroupAndOldLessonDateAndOldStartTime(GroupEntity group, LocalDate date, LocalTime startTime);

    List<ScheduleChangeEntity> findAllByGroupAndOldLessonDate(GroupEntity group, LocalDate date);

    List<ScheduleChangeEntity> findAllByGroup(GroupEntity group);
}

