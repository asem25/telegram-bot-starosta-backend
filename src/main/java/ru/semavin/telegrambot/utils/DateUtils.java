package ru.semavin.telegrambot.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
public final class DateUtils {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DateUtils() {
    }
    public static String getTodayWithCheckSunDay(){
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY){
            log.debug("Schedule get to SUNDAY, plus day to {}", FORMATTER.format(today));
            today = today.plusDays(1);
            log.debug("Schedule get to MONDAY, plus day to {}", FORMATTER.format(today));
        }
        return today.format(FORMATTER);
    }
    public static String getTomorrowWithCheckSunDay(){
        LocalDate today = LocalDate.now().plusDays(1);
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY){
            log.debug("Schedule get to SUNDAY, plus day to {}", FORMATTER.format(today));
            today = today.plusDays(1);
            log.debug("Schedule get to MONDAY, plus day to {}", FORMATTER.format(today));
        }
        return today.format(FORMATTER);
    }
}
