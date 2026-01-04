package ru.semavin.telegrambot.models.enums;

public enum LessonType {
    LECTURE,    // ЛК
    PRACTICAL,  // ПЗ
    EXAM,
    LAB;    // ЛР;

    public static LessonType valueOfString(String stringType) {
        return switch (stringType) {
            case "LECTURE" -> LECTURE;
            case "PRACTICAL" -> PRACTICAL;
            case "EXAM" -> EXAM;
            case "LAB" -> LAB;
            default -> null;
        };
    }

}
