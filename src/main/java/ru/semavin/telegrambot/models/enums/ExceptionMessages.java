package ru.semavin.telegrambot.models.enums;


import lombok.Getter;

/**
 * Перечисление стандартных сообщений для исключений.
 */
@Getter
public enum ExceptionMessages {
    GROUP_NOT_FOUND("Группа не найдена."),
    INVALID_DATE_FORMAT("Неверный формат даты. Ожидаемый формат: dd.MM.yyyy."),
    CONNECT_FAILED("Ошибка во время подключния"),
    SCHEDULE_NOT_FOUND("Расписания нет! Либо все закончилось, либо не началось..."),
    USER_NOT_FOUND("Человека не найдено!"),
    KEY_NOT_VALID("Ключ не совпадает"),
    USER_ALREADY_EXISTS_FOR_STAROSTA("Пользователь уже является старостой!"),
    USER_TELEGRAM_ID_EXISTS("Пользователь с таким Telegram_id уже есть!"),
    UNKNOWN_ERROR("Произошла неизвестная ошибка.");

    private final String message;

    ExceptionMessages(String message) {
        this.message = message;
    }

}
