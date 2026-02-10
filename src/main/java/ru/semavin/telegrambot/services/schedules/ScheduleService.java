package ru.semavin.telegrambot.services.schedules;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Сервис для работы с расписанием.
 */
@Service
@Slf4j
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleActualizationService scheduleActualizationService;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final SemesterService semesterService;
    private final GroupService groupService;
    private final ScheduleChangeService scheduleChangeService;
    private final ScheduleMergingService scheduleMergingService;
    private final Semaphore semaphore;
    private final ExecutorService executor;

    public ScheduleService(ScheduleRepository scheduleRepository, ScheduleActualizationService
                                   scheduleActualizationService,
                           ScheduleParserService scheduleParserService, ScheduleMapper scheduleMapper,
                           SemesterService semesterService, GroupService groupService,
                           ScheduleChangeService scheduleChangeService, ScheduleMergingService scheduleMergingService,
                           @Qualifier("groupSemaphore")
                           Semaphore semaphore, ExecutorService executor) {
        this.scheduleRepository = scheduleRepository;
        this.scheduleActualizationService = scheduleActualizationService;
        this.scheduleParserService = scheduleParserService;
        this.scheduleMapper = scheduleMapper;
        this.semesterService = semesterService;
        this.groupService = groupService;
        this.scheduleChangeService = scheduleChangeService;
        this.scheduleMergingService = scheduleMergingService;
        this.semaphore = semaphore;
        this.executor = executor;
    }

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
        Map<String, CompletableFuture<List<ScheduleDTO>>> scheduleGroupChunks =
                scheduleGroups.stream().collect(
                        Collectors.toMap(
                                group -> group,
                                group -> CompletableFuture.supplyAsync(() -> {
                                    semaphore.acquireUninterruptibly();
                                    try {
                                        return scheduleActualizationService
                                                .getActualSchedule(group, teacherUUID);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                        throw new RuntimeException(e);
                                    } finally {
                                        semaphore.release();
                                    }
                                }, executor)
                        )
                );
        Map<String, List<ScheduleDTO>> res = new HashMap<>(scheduleGroupChunks.size());

        CompletableFuture.allOf(scheduleGroupChunks.values().toArray(new CompletableFuture[0])).join();

        scheduleGroupChunks.forEach((groupName, schedules) ->
                res.put(groupName, schedules.join()));
        return scheduleMergingService.mergeMultiGroups(
                res
        );
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
