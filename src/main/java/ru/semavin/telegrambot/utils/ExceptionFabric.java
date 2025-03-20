package ru.semavin.telegrambot.utils;

import java.lang.reflect.Constructor;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;

/**
 * Универсальная фабрика исключений с предопределенными сообщениями.
 */
public final class ExceptionFabric {

    private ExceptionFabric() {
        // Запрещаем создание экземпляра класса
    }

    /**
     * Создает исключение с предопределенным сообщением.
     *
     * @param exceptionClass Класс исключения.
     * @param messageType Тип сообщения из {@link ExceptionMessages}.
     * @return Экземпляр исключения.
     */
    public static <T extends RuntimeException> T create(Class<T> exceptionClass, ExceptionMessages messageType) {
        try {
            Constructor<T> constructor = exceptionClass.getConstructor(String.class);
            return constructor.newInstance(messageType.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("Не удалось создать исключение: " + exceptionClass.getSimpleName(), e);
        }
    }
}
