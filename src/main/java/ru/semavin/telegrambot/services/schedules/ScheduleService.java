package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
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

    /**
     * Получает расписание для указанной группы и недели из БД.
     * Если расписания нет, загружает актуальное.
     */
    @Transactional
    @Cacheable(value = "scheduleCache", key = "#groupName + '-' + #week")
    public List<ScheduleDTO> getScheduleFromDataBase(String groupName, String week) {
        String actualWeek = (week != null) ? week : semesterService.getCurrentWeek();
        int currentWeek = Integer.parseInt(actualWeek);

        // Загружаем расписание из БД
        List<ScheduleEntity> existingSchedule = scheduleRepository.findAllByGroupNameIgnoreCaseAndLessonWeek(groupName, currentWeek);

        // Если расписание уже есть – возвращаем
        if (!existingSchedule.isEmpty()) {
            return scheduleMapper.toScheduleDTOList(existingSchedule);
        }

        // Если расписание отсутствует – загружаем и сохраняем
        return getActualSchedule(groupName, actualWeek);
    }

    /**
     * Получает актуальное расписание для указанной группы и недели, парсит сайт и сохраняет в БД.
     */
    @Transactional
    @CacheEvict(value = "scheduleCache", key = "#groupName + '-' + #week")
    public List<ScheduleDTO> getActualSchedule(String groupName, String week) {
        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return scheduleParserService.findScheduleByGroup(groupName, week);
            } catch (IOException e) {
                log.error("Ошибка при парсинге расписания для группы {}: {}", groupName, e.getMessage(), e);
                return new ArrayList<>();
            }
        });

        List<ScheduleEntity> scheduleEntities = future.join();
        //Чистим, чтобы не было лишних дней
        int currentWeek = Integer.parseInt(week);
        scheduleRepository.deleteAllByGroupNameIgnoreCaseAndLessonWeek(groupName, currentWeek);

        log.info("Найдено на маёвском сайте: {}", scheduleEntities);
        // Сохраняем новые записи
        List<ScheduleEntity> savedEntities = scheduleRepository.saveAllAndFlush(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }

    /**
     * Получает расписание на текущий день.
     * Если расписания нет в БД, загружает расписание за неделю и затем достаёт текущий день.
     */
    @Transactional
    public List<ScheduleDTO> getScheduleForCurrentDay(String groupName) {
        LocalDate today = LocalDate.now();
        int currentWeek = Integer.parseInt(semesterService.getCurrentWeek());

        // Проверяем, есть ли расписание на текущий день
        boolean existingSchedule = scheduleRepository.existsByLessonDateAndGroupNameIgnoreCase(today, groupName);
        if (!existingSchedule) {
            // Если расписания нет, загружаем неделю
            getScheduleFromDataBase(groupName, String.valueOf(currentWeek));
        }

        // После загрузки недели ищем расписание на текущий день
        List<ScheduleEntity> updatedSchedule = scheduleRepository.findAllByLessonDateAndGroupName(today, groupName);

        return scheduleMapper.toScheduleDTOList(updatedSchedule);
    }
}
