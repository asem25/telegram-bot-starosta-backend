package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleParserService scheduleParserService;
    private final ScheduleMapper scheduleMapper;
    private final SemesterService semesterService;

    /**
     * Получение расписания из БД или, если оно отсутствует, загрузка актуального
     */
    public List<ScheduleDTO> getScheduleFromDataBase(String groupName, String week) {
        String actualWeek = (week != null) ? week : semesterService.getCurrentWeek();
        List<ScheduleDTO> scheduleDTOS = scheduleMapper.toScheduleDTOList(
                scheduleRepository.findAllByGroupNameIgnoreCaseAndLessonWeek(groupName, Integer.parseInt(actualWeek))
        );

        return scheduleDTOS.isEmpty() ? getActualSchedule(groupName, actualWeek) : scheduleDTOS;
    }

    /**
     * Получение актуального расписания с использованием многопоточности
     */
    public List<ScheduleDTO> getActualSchedule(String groupName, String week) {
        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return scheduleParserService.findScheduleByGroup(groupName, week);
            } catch (IOException e) {
                log.error("Ошибка при парсинге расписания для группы {}: {}", groupName, e.getMessage(), e);
                return new ArrayList<>();
            }
        });

        List<ScheduleEntity> scheduleEntities = future.join(); // Ожидание завершения задачи
        return scheduleMapper.toScheduleDTOList(scheduleRepository.saveAll(scheduleEntities));
    }

    /**
     * Получение расписания на текущий день с параллельной загрузкой
     */
    public List<ScheduleDTO> getScheduleForCurrentDay(String groupName) {
        LocalDate now = LocalDate.now();
        List<ScheduleEntity> schedules = scheduleRepository.findAllByLessonDateAndGroupName(now, groupName);

        if (!schedules.isEmpty()) {
            return scheduleMapper.toScheduleDTOList(schedules);
        }

        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return scheduleParserService.findScheduleByGroup(groupName);
            } catch (IOException e) {
                log.error("Ошибка при парсинге расписания для группы {} на текущий день: {}", groupName, e.getMessage(), e);
                return new ArrayList<>();
            }
        });

        List<ScheduleEntity> scheduleEntities = future.join();
        return scheduleMapper.toScheduleDTOList(scheduleRepository.saveAll(scheduleEntities));
    }
}
