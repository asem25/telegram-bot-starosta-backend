package ru.semavin.telegrambot.utils.exceptions;

public class ScheduleChangeConflictException extends RuntimeException {
    public ScheduleChangeConflictException(String message) {
        super(message);
    }
}
