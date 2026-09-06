package ru.semavin.telegrambot.utils.exceptions;

public class ScheduleChangeForbiddenException extends RuntimeException {
    public ScheduleChangeForbiddenException(String message) {
        super(message);
    }
}
