package ru.semavin.telegrambot.utils.exceptions;

public class InvalidFormatDateException extends RuntimeException{
    public InvalidFormatDateException(String message) {
        super(message);
    }
}
