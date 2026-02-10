package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@Slf4j
@RequiredArgsConstructor
public class DailyScheduleUpdateService {

    private final ScheduleActualizationService scheduleService;

    @Scheduled(cron = "${dailyscheduleupdate.cron}", zone = "Europe/Moscow")
    @CacheEvict(value = "scheduleDay", allEntries = true)
    public void updateDailySchedules() {
        val startTime = System.currentTimeMillis();
        log.info("Начало ежедневного обновления расписания.");
        List<String> groups = getAllGroups();

        for (String group : groups) {
            try {
                log.info("Обновление расписания для группы {}", group);
                scheduleService.actualizationScheduleGroup(group);
            } catch (Exception e) {
                log.error("Ошибка обновления расписания для группы {}: {}", group, e.getMessage(), e);
            }
        }
        val endTime = System.currentTimeMillis();
        log.info("Завершено обновление расписания. [{}]ms", (endTime - startTime));
    }

    /**
     * Возвращает список групп, для которых необходимо обновлять расписание.
     * Данный метод можно доработать для получения списка из БД или конфигурационного файла.
     *
     * @return список названий групп
     */
    private List<String> getAllGroups() {
        return List.of("М3О-403С-22");
    }
}
