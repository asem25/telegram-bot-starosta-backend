package ru.semavin.telegrambot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
    void deleteByGroupName(String groupName);

    List<ScheduleEntity> findAllByGroupNameIgnoreCase(String groupName);
    List<ScheduleEntity> findAllByGroupNameIgnoreCaseAndLessonWeek(String groupName, Integer lessonWeek);
    boolean existsAllByGroupNameIgnoreCase(String groupName);
    boolean existsByLessonDate(LocalDate lessonDate);
    /**
     * Проверяет, существует ли запись расписания с указанными параметрами.
     *
     * @param groupName   название группы
     * @param lessonDate  дата занятия
     * @param startTime   время начала занятия
     * @param subjectName название предмета
     * @return true, если такая запись существует, иначе false
     */
    boolean existsByGroupNameIgnoreCaseAndLessonDateAndStartTimeAndSubjectName(
            String groupName,
            LocalDate lessonDate,
            LocalTime startTime,
            String subjectName
    );
    List<ScheduleEntity> findAllByLessonDateAndGroupName(LocalDate lessonDate, String groupName);
    /**
     * Удаляет все записи расписания для указанной группы и недели.
     *
     * @param groupName название группы
     * @param lessonWeek неделя расписания, которую нужно удалить
     */
    @Modifying
    @Transactional
    void deleteAllByGroupNameIgnoreCaseAndLessonWeek(String groupName, int lessonWeek);
}
