package ru.semavin.telegrambot.services.schedules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.utils.DateUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Сервис для работы с данными семестра.
 * Дата начала семестра задаётся через конфигурацию (application.properties)
 * и используется для математического расчёта текущей учебной недели.
 */
@Slf4j
@Service
public class SemesterService {

    private final LocalDate semesterStart;



    /**
     * Конструктор, принимающий дату начала семестра в виде строки из конфигурации.
     *
     * @param semesterStartStr Дата начала семестра, заданная в конфигурационном файле, например "10.02.2025".
     */
    public SemesterService(@Value("${semester.start}") String semesterStartStr) {
        // Определяем формат даты (например, "dd.MM.yyyy")
        this.semesterStart = LocalDate.parse(semesterStartStr, DateUtils.FORMATTER);
        log.info("Дата начала семестра установлена через конфигурацию: {}", this.semesterStart);
    }

    /**
     * Вычисляет текущую учебную неделю на основе разницы между датой начала семестра и сегодняшней датой.
     *
     * @return номер недели (1 = первая неделя). Если текущая дата раньше начала семестра — возвращается 0.
     */
    public String getCurrentWeek() {
        LocalDate today = LocalDate.now();

        return getWeekForInputDate(DateUtils.FORMATTER.format(today));
    }

    /**
     * Возвращает форматированную дату форматтером
     *
     * @param date в виде строки
     * @return дату в формате заданном форматтере {@link DateUtils}
     */
    public LocalDate getFormatterDate(String date) {
        return LocalDate.parse(date, DateUtils.FORMATTER);
    }
    /**
     * Вычисляет номер учебной недели для заданной даты.
     * Если переданная дата меньше даты начала семестра, возвращается "0". Если дата приходится на воскресенье, неделя увеличивается на 1.
     *
     * @param dateStr Строковое представление даты (например, "19.02.2025") в формате "dd.MM.yyyy"
     * @return Номер учебной недели в виде строки.
     * @throws IllegalArgumentException если переданная дата имеет неверный формат.
     */
    public String getWeekForDate(String dateStr) {
        return getWeekForInputDate(dateStr);
    }
    /**
     * Вычисляет номер учебной недели для заданной даты.
     * Если переданная дата меньше даты начала семестра, возвращается "0". Если дата приходится на воскресенье, неделя увеличивается на 1.
     *
     * @param dateStr Строковое представление даты (например, "19.02.2025") в формате "dd.MM.yyyy"
     * @return Номер учебной недели в виде строки.
     * @throws IllegalArgumentException если переданная дата имеет неверный формат.
     */
    private String getWeekForInputDate(String dateStr) {
        LocalDate inputDate;
        try {
            inputDate = LocalDate.parse(dateStr, DateUtils.FORMATTER);
        } catch (Exception e) {
            log.error("Ошибка парсинга даты '{}': {}", dateStr, e.getMessage());
            throw new IllegalArgumentException("Неверный формат даты, требуется dd.MM.yyyy", e);
        }

        if (inputDate.isBefore(semesterStart)) {
            log.info("Дата {} раньше начала семестра {}", inputDate, semesterStart);
            return "0";
        }

        long daysBetween = ChronoUnit.DAYS.between(semesterStart, inputDate);
        int weekNumber = (int) (daysBetween / 7) + 1;
        if (inputDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            weekNumber++;
        }
        log.info("Для даты {}: разница в днях: {}, вычисленная неделя: {}", inputDate, daysBetween, weekNumber);
        return String.valueOf(weekNumber);
    }

}
