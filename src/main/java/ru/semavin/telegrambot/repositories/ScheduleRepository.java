package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    List<ScheduleEntity> findAllByGroupNameIgnoreCaseAndLessonWeek(String groupName, Integer lessonWeek);

    boolean existsByLessonDateAndGroupNameIgnoreCase(LocalDate date, String groupName);

    List<ScheduleEntity> findAllByLessonDateAndGroupName(LocalDate lessonDate, String groupName);
    /**
     * Удаляет все записи расписания для указанной группы и недели.
     *
     * @param groupName название группы
     * @param lessonWeek неделя расписания, которую нужно удалить
     */
    void deleteAllByGroupNameIgnoreCaseAndLessonWeek(String groupName, int lessonWeek);
}
