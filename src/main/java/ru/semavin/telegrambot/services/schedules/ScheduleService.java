package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    public List<ScheduleDTO> getActualSchedule(String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);

        List<ScheduleEntity> scheduleEntities = scheduleParserService.findScheduleByGroup(group);

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание для группы {} найдено", group);

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
        List<ScheduleChangeEntity> changes = scheduleChangeService.getChangesForDay(groupName, parsingDate);

        return mergeChanges(original, changes, parsingDate);
    }


    private List<ScheduleDTO> mergeChanges(
            List<ScheduleDTO> originalSchedule,
            List<ScheduleChangeEntity> changes,
            LocalDate targetDate
    ) {
        List<ScheduleDTO> merged = new ArrayList<>();

        for (ScheduleDTO dto : originalSchedule) {
            ScheduleChangeEntity ch = changes.stream()
                    .filter(change ->
                            change.getOldLessonDate().equals(dto.getLessonDate()) &&
                                    change.getOldStartTime().equals(dto.getStartTime()) &&
                                    change.getSubjectName().equalsIgnoreCase(dto.getSubjectName())
                    )
                    .findFirst()
                    .orElse(null);

            if (ch != null) {
                if (ch.isDeleted()) {
                    continue;
                }

                LocalDate newDate = ch.getNewLessonDate() != null ? ch.getNewLessonDate() : dto.getLessonDate();
                if (!newDate.equals(targetDate)) {
                    continue;
                }

                dto.setLessonDate(newDate);
                if (ch.getNewStartTime() != null) dto.setStartTime(ch.getNewStartTime());
                if (ch.getNewEndTime() != null) dto.setEndTime(ch.getNewEndTime());
                dto.setDescription(ch.getDescription());
            }

            merged.add(dto);
        }

        changes.stream()
                .filter(ch -> !ch.isDeleted()
                        && targetDate.equals(ch.getNewLessonDate())
                        && !targetDate.equals(ch.getOldLessonDate()))
                .forEach(ch -> {
                    ScheduleDTO dto = ScheduleDTO.builder()
                            .groupName(originalSchedule.get(0).getGroupName())
                            .subjectName(ch.getSubjectName())
                            .lessonDate(ch.getNewLessonDate())
                            .startTime(ch.getNewStartTime())
                            .endTime(ch.getNewEndTime())
                            .description(ch.getDescription())
                            .build();
                    merged.add(dto);
                });

        merged.sort(Comparator.comparing(ScheduleDTO::getStartTime));
        return merged;
    }

    public ScheduleDTO findLesson(String groupName, String date, String startTime) {
        GroupEntity group = groupService.findEntityByName(groupName);
        LocalDate lessonDate = semesterService.getFormatterDate(date);
        LocalTime time = LocalTime.parse(startTime, DateTimeFormatter.ofPattern("HH:mm"));
        return scheduleMapper.toScheduleDTO(scheduleRepository.findByGroupAndLessonDateAndStartTime(group, lessonDate, time));
    }
}
