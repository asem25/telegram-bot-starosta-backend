package ru.semavin.telegrambot.services.schedules;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    private final UserService userService;
    @Getter
    private LocalDateTime lastUpdateStudents = LocalDateTime.now();
    @Getter
    private LocalDateTime lastUpdateTeacher = LocalDateTime.now();

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    public List<ScheduleDTO> getActualSchedule(String groupName) {
        lastUpdateStudents = LocalDateTime.now();
        GroupEntity group = groupService.findEntityByName(groupName);
        List<ScheduleEntity> scheduleEntities = scheduleParserService.findScheduleByGroup(group);

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание для группы {} найдено", group);

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAllAndFlush(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }

    @Transactional
    public List<ScheduleDTO> getActualByTeacher(UserEntity user) {
        lastUpdateTeacher = LocalDateTime.now();
        List<ScheduleEntity> scheduleEntities = scheduleParserService
                .getScheduleTeacherFromSite(user.getTeacherUuid());

        scheduleRepository.deleteAllByTeacher(user);

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAllAndFlush(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
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
        List<ScheduleChangeEntity> changes = scheduleChangeService.getChangesByGroup(groupName);

        return mergeChanges(original, changes, parsingDate);
    }

    //todo cacheble
    public List<ScheduleDTO> getScheduleForTeacher(String uuidTeacher) {
        return scheduleMapper.toScheduleDTOList(
                scheduleRepository.findAllByTeacher(
                        userService.findTeacher(uuidTeacher)
                )
        );
    }

    public List<ScheduleDTO> getScheduleForGroup(String groupName) {
        return scheduleMapper.toScheduleDTOList(
                scheduleRepository.findAllByGroup(
                        groupService.findEntityByName(groupName))
        );
    }

    private List<ScheduleDTO> mergeChanges(
            List<ScheduleDTO> originalSchedule,
            List<ScheduleChangeEntity> changes,
            LocalDate parsingDate
    ) {
        Map<String, ScheduleChangeEntity> controlSums =
                changes.stream().collect(
                        Collectors.toMap(
                                ScheduleChangeEntity::getOldControlSum,
                                obj -> obj
                        )
                );
        List<ScheduleDTO> merged = new ArrayList<>();

        changes.forEach(change -> {
            //todo newLessonDate/startTime/endTime не заполняются
            if (change.getNewLessonDate() != null &&
            parsingDate.equals(change.getNewLessonDate())) {

                val newdto = ScheduleDTO.builder()
                        .classroom(change.getClassroom())
                        .controlSum(change.getOldControlSum())
                        .teacherName(change.getTeacherName())
                        .subjectName(change.getSubjectName())
                        .lessonType(change.getLessonType())
                        .groupName(change.getGroup().getGroupName())
                        .lessonDate(change.getNewLessonDate())
                        .build();

                if (change.getNewEndTime() != null) {
                    newdto.setEndTime(change.getNewEndTime());
                } else {
                    newdto.setEndTime(change.getOldEndTime());
                }

                if (change.getNewStartTime() != null) {
                    newdto.setStartTime(change.getNewStartTime());
                } else {
                    newdto.setStartTime(change.getOldStartTime());
                }

                if (change.getDescription() != null) {
                    newdto.setDescription(change.getDescription());
                }

                merged.add(newdto);
            }
        });

        //todo если перенести пару на субботу, но в субботу нет пар кроме перенесенной, то пара не отобразиться
        for (ScheduleDTO dto : originalSchedule) {
            val controlSum = dto.getControlSum();
            if (controlSums.containsKey(controlSum)) {
                val change = controlSums.get(controlSum);

                if (change.isDeleted()) {
                    continue;
                }

                if (change.getNewLessonDate() != null) {
                    dto.setLessonDate(change.getNewLessonDate());
                }

                if (change.getNewStartTime() != null) {
                    dto.setStartTime(change.getNewStartTime());
                }

                if (change.getNewEndTime() != null) {
                    dto.setEndTime(change.getNewEndTime());
                }

                if (change.getDescription() != null) {
                    dto.setDescription(change.getDescription());
                }

                if (!change.getClassroom().equals(dto.getClassroom())) {
                    dto.setClassroom(change.getClassroom());
                }
            }

            merged.add(dto);
        }

        return merged;
    }

    public ScheduleDTO findLesson(String groupName, String date, String startTime) {
        GroupEntity group = groupService.findEntityByName(groupName);
        LocalDate lessonDate = semesterService.getFormatterDate(date);
        LocalTime time = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        return scheduleMapper.toScheduleDTO(scheduleRepository.findByGroupAndLessonDateAndStartTime(group, lessonDate, time));
    }
}
