package ru.semavin.telegrambot.services.schedules;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.cache.CacheUtil;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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

    @PostConstruct
    public void init() {
    }
    /**
     * Получает расписание для указанной группы и недели из БД.
     * Если расписания нет, загружает актуальное.
     */
    @Transactional
    @Deprecated
    public List<ScheduleDTO> getScheduleFromDataBase(String groupName) {
        //Бесполезен
        return getActualSchedule(groupName);
    }

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    public List<ScheduleDTO> getActualSchedule(String groupName) {
        ;

        GroupEntity group = groupService.findEntityByName(groupName);
        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> scheduleParserService.findScheduleByGroup(group));

        List<ScheduleEntity> scheduleEntities = future.join();

        scheduleRepository.deleteAllByGroup(group);

        log.info("Расписание найдено");
        // Сохраняем новые записи
        List<ScheduleEntity> savedEntities = scheduleRepository.saveAllAndFlush(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }

    /**
     * Возвращает расписание за заданный день
     * Кэширование + проверка в БД
     * @param groupName номер группы
     * @param date дата в виде строки
     * @return возвращает список DTO расписаний
     */

    @Transactional
    @Cacheable(value = "scheduleDay", key = "#groupName + '-' + #date", unless = "#result == null")
    public List<ScheduleDTO> getScheduleForDay(String groupName, String date) {
        LocalDate parsingDate = semesterService.getFormatterDate(date);

        GroupEntity group = groupService.findEntityByName(groupName);


        // После загрузки недели ищем расписание на текущий день
        List<ScheduleEntity> updatedSchedule = scheduleRepository.findAllByLessonDateAndGroup(parsingDate, group);

        return scheduleMapper.toScheduleDTOList(updatedSchedule);
    }
}
