package ru.semavin.telegrambot.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        this.semesterStart = LocalDate.parse(semesterStartStr, fmt);
        log.info("Дата начала семестра установлена через конфигурацию: {}", this.semesterStart);
    }

    /**
     * Вычисляет текущую учебную неделю на основе разницы между датой начала семестра и сегодняшней датой.
     *
     * @return номер недели (1 = первая неделя). Если текущая дата раньше начала семестра — возвращается 0.
     */
    public String getCurrentWeek() {
        LocalDate today = LocalDate.now();
        if (today.isBefore(semesterStart)) {
            log.info("Семестр ещё не начался: сегодня {} < {}", today, semesterStart);
            return "0";
        }

        long daysBetween = ChronoUnit.DAYS.between(semesterStart, today);
        int weekNumber = (int) (daysBetween / 7) + 1;
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY){
            weekNumber++;
        }
        log.info("Сегодня: {}, разница в днях: {}, текущая неделя: {}", today, daysBetween, weekNumber);
        return String.valueOf(weekNumber);
    }
}
