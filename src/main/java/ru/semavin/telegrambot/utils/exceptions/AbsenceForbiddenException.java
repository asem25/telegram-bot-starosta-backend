package ru.semavin.telegrambot.utils.exceptions;

public class AbsenceForbiddenException extends RuntimeException {
    public AbsenceForbiddenException(String message) {
        super(message);
    }
}
