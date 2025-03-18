package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

/**
 * Сервис для работы с расписанием.
 *
 * <p>При получении актуального расписания для группы происходит удаление старых записей для заданной недели и сохранение новых данных в транзакции.</p>
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
     * Получает расписание для указанной группы и недели из БД. Если записи отсутствуют, загружает актуальное расписание.
     *
     * @param groupName название группы
     * @param week      неделя расписания
     * @return список DTO расписания
     */
    @Transactional
    public List<ScheduleDTO> getScheduleFromDataBase(String groupName, String week) {
        String actualWeek = (week != null) ? week : semesterService.getCurrentWeek();
        List<ScheduleDTO> scheduleDTOS = scheduleMapper.toScheduleDTOList(
                scheduleRepository.findAllByGroupNameIgnoreCaseAndLessonWeek(groupName, Integer.parseInt(actualWeek))
        );
        return scheduleDTOS.isEmpty() ? getActualSchedule(groupName, actualWeek) : scheduleDTOS;
    }

    /**
     * Получает актуальное расписание для указанной группы и недели.
     *
     * <p>Перед сохранением новых данных происходит удаление старых записей для данной группы и недели.
     * Сохранение новых записей выполняется в отдельном транзакционном методе.</p>
     *
     * @param groupName название группы
     * @param week      неделя расписания
     * @return список DTO нового расписания
     */
    @Transactional
    public List<ScheduleDTO> getActualSchedule(String groupName, String week) {
        int currentWeek = Integer.parseInt(week);

        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return scheduleParserService.findScheduleByGroup(groupName, week);
            } catch (IOException e) {
                log.error("Ошибка при парсинге расписания для группы {}: {}", groupName, e.getMessage(), e);
                return new ArrayList<>();
            }
        });

        List<ScheduleEntity> scheduleEntities = future.join();

        List<ScheduleEntity> newEntities = scheduleEntities.parallelStream()
                .filter(entity -> {
                    boolean exists = scheduleRepository
                            .existsByGroupNameIgnoreCaseAndLessonDateAndStartTimeAndSubjectName(
                                    entity.getGroupName(),
                                    entity.getLessonDate(),
                                    entity.getStartTime(),
                                    entity.getSubjectName()
                            );
                    if (exists) {
                        log.debug("Пропускаем дубликат: {}", entity);
                    }
                    return !exists;
                })
                .collect(Collectors.toList());


        scheduleRepository.deleteAllByGroupNameIgnoreCaseAndLessonWeek(groupName, currentWeek);

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAll(newEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }

    /**
     * Получает расписание на текущий день для указанной группы.
     *
     * @param groupName название группы
     * @return список DTO расписания на текущий день
     */
    @Transactional
    public List<ScheduleDTO> getScheduleForCurrentDay(String groupName) {
        LocalDate now = LocalDate.now();
        List<ScheduleEntity> schedules = scheduleRepository.findAllByLessonDateAndGroupName(now, groupName);

        if (!schedules.isEmpty()) {
            return scheduleMapper.toScheduleDTOList(schedules);
        }

        CompletableFuture<List<ScheduleEntity>> future = CompletableFuture.supplyAsync(() -> {
            try {
                return scheduleParserService.findScheduleByGroupAndDate(groupName, now);
            } catch (IOException e) {
                log.error("Ошибка при парсинге расписания для группы {} на текущий день: {}", groupName, e.getMessage(), e);
                return new ArrayList<>();
            }
        });

        List<ScheduleEntity> scheduleEntities = future.join();

        List<ScheduleEntity> savedEntities = scheduleRepository.saveAll(scheduleEntities);

        return scheduleMapper.toScheduleDTOList(savedEntities);
    }


}
