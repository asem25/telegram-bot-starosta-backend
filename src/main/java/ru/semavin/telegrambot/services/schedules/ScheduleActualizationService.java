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

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleActualizationService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final GroupService groupService;

    @Transactional
    @CacheEvict(value = {"scheduleCache", "scheduleDay"}, allEntries = true)
    public void actualizationScheduleGroup(String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        List<ScheduleEntity> scheduleEntities = scheduleParserService.findScheduleByGroup(group);

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание для группы {} найдено", group);

        scheduleRepository.saveAllAndFlush(scheduleEntities);

        log.info("БД очищена. Расписание группы [{}] загружено.", groupName);
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
