package ru.semavin.telegrambot.utils.exceptions;

public class DeadlineNotFoundException extends RuntimeException {
    public DeadlineNotFoundException(String message) {
        super(message);
    }
}
