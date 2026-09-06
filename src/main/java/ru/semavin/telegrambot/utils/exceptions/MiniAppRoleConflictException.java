package ru.semavin.telegrambot.utils.exceptions;

public class MiniAppRoleConflictException extends RuntimeException {
    public MiniAppRoleConflictException(String message) {
        super(message);
    }
}
