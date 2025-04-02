package ru.semavin.telegrambot.utils.exceptions;

public class UserAlreadyNotExistsForStarostaException extends RuntimeException {
    public UserAlreadyNotExistsForStarostaException(String message) {
        super(message);
    }
}
