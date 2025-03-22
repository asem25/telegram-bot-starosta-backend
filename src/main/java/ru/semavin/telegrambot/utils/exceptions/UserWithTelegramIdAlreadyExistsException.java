package ru.semavin.telegrambot.utils.exceptions;

public class UserWithTelegramIdAlreadyExistsException extends RuntimeException {
    public UserWithTelegramIdAlreadyExistsException(String message) {
        super(message);
    }
}
