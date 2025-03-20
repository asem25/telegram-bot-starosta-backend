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
    UNKNOWN_ERROR("Произошла неизвестная ошибка.");

    private final String message;

    ExceptionMessages(String message) {
        this.message = message;
    }

}
