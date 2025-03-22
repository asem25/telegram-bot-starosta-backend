package ru.semavin.telegrambot.utils.exceptions;

public class UserAlreadyExistsForStarostaException extends RuntimeException {
    public UserAlreadyExistsForStarostaException(String message) {
        super(message);
    }
}
