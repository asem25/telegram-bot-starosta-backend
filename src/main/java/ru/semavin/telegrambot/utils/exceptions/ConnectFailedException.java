package ru.semavin.telegrambot.utils.exceptions;

public class ConnectFailedException extends RuntimeException{
    public ConnectFailedException(String message) {
        super(message);
    }
}
