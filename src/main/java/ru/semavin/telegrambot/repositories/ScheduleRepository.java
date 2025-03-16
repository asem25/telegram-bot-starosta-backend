package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    void deleteByGroupName(String groupName);

    List<ScheduleEntity> findAllByGroupNameIgnoreCase(String groupName);
    List<ScheduleEntity> findAllByGroupNameIgnoreCaseAndLessonWeek(String groupName, Integer lessonWeek);
}
