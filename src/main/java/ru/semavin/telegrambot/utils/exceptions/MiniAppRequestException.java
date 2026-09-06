package ru.semavin.telegrambot.utils.exceptions;

public class MiniAppRequestException extends RuntimeException {
    public MiniAppRequestException(String message) {
        super(message);
    }
}
