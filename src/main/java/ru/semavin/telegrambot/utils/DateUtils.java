package ru.semavin.telegrambot.utils;

import java.time.format.DateTimeFormatter;

public final class DateUtils {
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private DateUtils() {
    }
}
