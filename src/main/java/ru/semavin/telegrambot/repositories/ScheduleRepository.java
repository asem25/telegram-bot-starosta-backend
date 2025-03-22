package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    List<ScheduleEntity> findAllByGroupAndLessonWeek(GroupEntity group, Integer lessonWeek);

    boolean existsByLessonDateAndGroup(LocalDate lessonDate, GroupEntity group);

    List<ScheduleEntity> findAllByLessonDateAndGroup(LocalDate lessonDate, GroupEntity group);
    /**
     * Удаляет все записи расписания для указанной группы и недели.
     *
     * @param group название группы
     * @param lessonWeek неделя расписания, которую нужно удалить
     */
    void deleteAllByGroupAndLessonWeek(GroupEntity group, Integer lessonWeek);
}
