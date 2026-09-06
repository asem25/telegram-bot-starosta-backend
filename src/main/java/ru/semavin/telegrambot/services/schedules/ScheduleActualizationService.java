package ru.semavin.telegrambot.services.schedules;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleActualizationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final GroupService groupService;
    private final ScheduleSyncStatusService scheduleSyncStatusService;

    @Transactional
    @CacheEvict(value = {"scheduleCache", "scheduleDay"}, allEntries = true)
    public void actualizationScheduleGroup(String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        List<ScheduleEntity> scheduleEntities = scheduleParserService.findScheduleByGroup(group);

        Map<String, ScheduleEntity> existingByControlSum = scheduleRepository.findAllByGroup(group).stream()
                .filter(entity -> entity.getControlSum() != null)
                .collect(Collectors.toMap(
                        ScheduleEntity::getControlSum,
                        Function.identity(),
                        (first, ignored) -> first
                ));
        List<ScheduleEntity> reconciledSchedule = scheduleEntities.stream()
                .map(parsed -> reconcileIdentity(parsed, existingByControlSum.get(parsed.getControlSum())))
                .toList();

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание для группы {} найдено", group);

        scheduleRepository.saveAllAndFlush(reconciledSchedule);

        scheduleSyncStatusService.markSuccessfulSync(groupName);

        log.info("Расписание группы [{}] актуализировано.", groupName);
    }

    private ScheduleEntity reconcileIdentity(ScheduleEntity parsed, ScheduleEntity existing) {
        if (existing == null) {
            return parsed;
        }
        return parsed.toBuilder()
                .occurrenceId(existing.getOccurrenceId())
                .seriesId(existing.getSeriesId())
                .build();
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

}
