package ru.semavin.telegrambot.services.schedules;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.util.List;
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
    @Cacheable(value = "scheduleCache", key = "#groupName + '-' + #week", unless = "#result == null or #result.isEmpty()")
    public List<ScheduleDTO> getScheduleFromDataBase(String groupName, String week) {
        String actualWeek = (week != null) ? week : semesterService.getCurrentWeek();
        int currentWeek = Integer.parseInt(actualWeek);

        GroupEntity group = groupService.findEntityByName(groupName);

        // Загружаем расписание из БД
        List<ScheduleEntity> existingSchedule = scheduleRepository.findAllByGroupAndLessonWeek(group, currentWeek);

        // Если расписание уже есть – возвращаем
        if (!existingSchedule.isEmpty()) {
            return scheduleMapper.toScheduleDTOList(existingSchedule);
        }

        // Если расписание отсутствует – загружаем и сохраняем
        return getActualSchedule(groupName);
    }

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    @CacheEvict(value = "scheduleCache", key = "#groupName")
    public List<ScheduleDTO> getActualSchedule(String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> scheduleParserService.findScheduleByGroup(group));

        List<ScheduleEntity> scheduleEntities = future.join();

        scheduleRepository.deleteAllByGroup(group);

        log.info("Найдено на маёвском сайте: {}", scheduleEntities);
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
    @Cacheable(value = "scheduleDay", key ="#groupName + '-' + #date", unless = "#result == null or #result.isEmpty()")
    public List<ScheduleDTO> getScheduleForDay(String groupName, String date) {
        LocalDate parsingDate = semesterService.getFormatterDate(date);
        int actualWeek = Integer.parseInt(semesterService.getWeekForDate(date));

        GroupEntity group = groupService.findEntityByName(groupName);

        // Проверяем, есть ли расписание на текущий день
        boolean existingSchedule = scheduleRepository.existsByLessonDateAndGroup(parsingDate, group);
        if (!existingSchedule) {
            // Если расписания нет, загружаем неделю
            getScheduleFromDataBase(groupName, String.valueOf(actualWeek));
        }

        // После загрузки недели ищем расписание на текущий день
        List<ScheduleEntity> updatedSchedule = scheduleRepository.findAllByLessonDateAndGroup(parsingDate, group);

        return scheduleMapper.toScheduleDTOList(updatedSchedule);
    }
}
