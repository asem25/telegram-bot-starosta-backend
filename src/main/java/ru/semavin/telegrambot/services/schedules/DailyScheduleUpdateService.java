package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Сервис для ежедневного обновления расписания.
 *
 * <p>Метод {@link #updateDailySchedules()} запускается по расписанию (ежедневно в 20:00 вечера) и для каждого заданного имени группы
 * получает актуальное расписание с сайта. При этом используется метод {@link ScheduleService#getActualSchedule(String, String)},
 * который отвечает за очистку старых записей и сохранение новых в транзакционном контексте.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DailyScheduleUpdateService {

    private final ScheduleService scheduleService;
    private final SemesterService semesterService; // Для получения текущей недели

    /**
     * Метод, запускаемый ежедневно в 20:00 вечера.
     * В этом методе происходит выгрузка расписания для всех групп, указанных в {@link #getAllGroups()}.
     */
    @Scheduled(cron = "0 0 20 * * *")

    public void updateDailySchedules() {
        log.info("Начало ежедневного обновления расписания.");
        String currentWeek = semesterService.getCurrentWeek();
        List<String> groups = getAllGroups();

        for (String group : groups) {
            try {
                log.info("Обновление расписания для группы {} на неделю {}", group, currentWeek);
                scheduleService.getActualSchedule(group, currentWeek);
            } catch (Exception e) {
                log.error("Ошибка обновления расписания для группы {}: {}", group, e.getMessage(), e);
            }
        }
        log.info("Завершено обновление расписания.");
    }

    /**
     * Возвращает список групп, для которых необходимо обновлять расписание.
     * Данный метод можно доработать для получения списка из БД или конфигурационного файла.
     *
     * @return список названий групп
     */
    private List<String> getAllGroups() {
        //TODO в большом приложение возможно, что будет очень много групп, нужно удалять неактуальные
        return Arrays.asList("М3О-303С-22");
    }
}
