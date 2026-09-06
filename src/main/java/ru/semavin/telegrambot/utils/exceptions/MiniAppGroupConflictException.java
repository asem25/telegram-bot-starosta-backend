package ru.semavin.telegrambot.utils.exceptions;

public class MiniAppGroupConflictException extends RuntimeException {
    public MiniAppGroupConflictException(String message) {
        super(message);
    }
}
