package ru.semavin.telegrambot.services.schedules;

import ru.semavin.telegrambot.models.ScheduleEntity;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public final class ScheduleSeriesIdService {
    private static final String TEACHER_NOT_ASSIGNED = "Не задан";

    private ScheduleSeriesIdService() {
    }

    public static UUID resolve(ScheduleEntity lesson) {
        return lesson.getSeriesId() == null ? calculate(lesson) : lesson.getSeriesId();
    }

    public static UUID calculate(ScheduleEntity lesson) {
        String canonicalKey = buildCanonicalKey(lesson);
        return UUID.nameUUIDFromBytes(canonicalKey.getBytes(StandardCharsets.UTF_8));
    }

    private static String buildCanonicalKey(ScheduleEntity lesson) {
        return String.join("|",
                semesterKey(lesson),
                lesson.getGroup().getGroupName(),
                lesson.getLessonDate().getDayOfWeek().name(),
                lesson.getStartTime().toString(),
                lesson.getEndTime().toString(),
                Objects.toString(lesson.getSubjectName(), ""),
                lesson.getLessonType() == null ? "" : lesson.getLessonType().name(),
                teacherKey(lesson)
        );
    }

    private static String semesterKey(ScheduleEntity lesson) {
        return lesson.getLessonDate().getYear() + "-"
                + (lesson.getLessonDate().getMonthValue() < 8 ? "SPRING" : "AUTUMN");
    }

    private static String teacherKey(ScheduleEntity lesson) {
        if (lesson.getTeacher() == null || lesson.getTeacher().getTeacherUuid() == null) {
            return TEACHER_NOT_ASSIGNED;
        }
        return lesson.getTeacher().getTeacherUuid();
    }
}
