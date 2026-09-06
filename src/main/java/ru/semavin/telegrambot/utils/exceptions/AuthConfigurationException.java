package ru.semavin.telegrambot.utils.exceptions;

/**
 * Signals that Telegram authentication is temporarily unavailable because the
 * server-side authentication configuration is incomplete or invalid.
 */
public class AuthConfigurationException extends RuntimeException {
    public AuthConfigurationException(String message) {
        super(message);
    }
}
