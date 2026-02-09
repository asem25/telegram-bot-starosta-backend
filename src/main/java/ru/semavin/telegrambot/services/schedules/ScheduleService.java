package ru.semavin.telegrambot.services.schedules;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Сервис для работы с расписанием.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final SemesterService semesterService;
    private final GroupService groupService;
    private final ScheduleChangeService scheduleChangeService;
    private final ScheduleMergingService scheduleMergingService;
    private final Semaphore semaphore;

    @Getter
    private LocalDateTime lastUpdateStudents = LocalDateTime.now();
    @Getter
    private LocalDateTime lastUpdateTeacher = LocalDateTime.now();

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    @CacheEvict(value = {"scheduleCache", "scheduleDay"}, allEntries = true)
    public synchronized List<ScheduleDTO> getActualSchedule(String groupName) {
        lastUpdateStudents = LocalDateTime.now();
        GroupEntity group = groupService.findEntityByName(groupName);
        List<ScheduleEntity> scheduleEntities = scheduleParserService.findScheduleByGroup(group);

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание для группы {} найдено", group);

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAllAndFlush(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }

    @Transactional
    public List<ScheduleDTO> getActualSchedule(String groupName, String teacherUUID) {
        GroupEntity group = groupService.findEntityByName(groupName);
        log.debug("Парсинг расписания группы [{}].", groupName);
        val scheduleAfterParsing = scheduleParserService.findScheduleByGroup(group)
                .stream().filter(sch ->
                        sch.getTeacher().getTeacherUuid().equals(teacherUUID))
                .toList();
        return scheduleMapper.toScheduleDTOList(
                scheduleAfterParsing);
    }

    /**
     * Возвращает расписание за заданный день
     * Кэширование + проверка в БД
     *
     * @param groupName номер группы
     * @param date      дата в виде строки
     * @return возвращает список DTO расписаний
     */

    @Transactional
    @Cacheable(value = "scheduleDay", key = "#groupName + '-' + #date", unless = "#result == null")
    public List<ScheduleDTO> getScheduleForDay(String groupName, String date) {
        LocalDate parsingDate = semesterService.getFormatterDate(date);

        GroupEntity group = groupService.findEntityByName(groupName);

        List<ScheduleEntity> updatedSchedule = scheduleRepository.findAllByLessonDateAndGroup(parsingDate, group);
        List<ScheduleDTO> original = scheduleMapper.toScheduleDTOList(updatedSchedule);
        List<ScheduleChangeEntity> changes = scheduleChangeService.getChangesDtoAnyDay(groupName, parsingDate);

        return scheduleMergingService.mergeChanges(original, changes, parsingDate);
    }

    @Transactional
    public List<ScheduleDTO> getTeacherSchedule(String teacherUUID) {
        val scheduleGroups = scheduleParserService.findTeacherGroups(teacherUUID);
        Map<String, List<ScheduleDTO>> scheduleGroupChunks =
                scheduleGroups.stream().collect(
                        Collectors.toMap(
                                group -> group,
                                group -> getActualSchedule(group, teacherUUID)
                        )
                );

        return scheduleMergingService.mergeMultiGroups(scheduleGroupChunks);
    }

    public List<ScheduleDTO> getScheduleForISC(String groupName) {
        return scheduleMergingService.getScheduleAfterMerge(groupName);
    }

    public ScheduleDTO findLesson(String groupName, String date, String startTime) {
        GroupEntity group = groupService.findEntityByName(groupName);
        LocalDate lessonDate = semesterService.getFormatterDate(date);
        LocalTime time = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        return scheduleMapper.toScheduleDTO(scheduleRepository.findByGroupAndLessonDateAndStartTime(group, lessonDate, time));
    }

}
