package ru.semavin.telegrambot.services.schedules;

import lombok.val;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;


public final class ScheduleControlSumParserService {

    private ScheduleControlSumParserService() {

    }

    public static ScheduleEntity fillCalculateSum(ScheduleEntity scheduleEntity) {
        scheduleEntity.setControlSum(getHashEntity(scheduleEntity));
        return scheduleEntity;
    }

    public static ScheduleChangeEntity fillCalculateSum(ScheduleChangeEntity entity) {
        entity.setOldControlSum(getHashEntity(entity));
        return entity;
    }

    private static String getHashEntity(ScheduleEntity scheduleEntity) {
        val payload = toCanonicalString(scheduleEntity);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    private static String getHashEntity(ScheduleChangeEntity entity) {
        val payload = toCanonicalString(entity);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    /**
     * Формирует строку для вычисления контрольной суммы
     */
    private static String toCanonicalString(ScheduleEntity entity) {
        return String.join("|",
                entity.getLessonDate().toString(),
                entity.getStartTime().toString(),
                entity.getEndTime().toString(),
                entity.getGroup().getGroupName(),
                nullToEmpty(entity.getSubjectName()),
                nullToEmpty(entity.getTeacher().getFirstName()),
                nullToEmpty(entity.getTeacher().getLastName()),
                nullToEmpty(entity.getTeacher().getPatronymic()),
                nullToEmpty(entity.getClassroom()),
                nullToEmpty(entity.getLessonType().name())
        );
    }

    private static String toCanonicalString(ScheduleChangeEntity dto) {
        String[] teacherName = dto.getTeacherName().split(" ");
        return String.join("|",
                dto.getOldLessonDate().toString(),
                dto.getOldStartTime().toString(),
                dto.getOldEndTime().toString(),
                dto.getGroup().getGroupName(),
                nullToEmpty(dto.getSubjectName()),
                nullToEmpty(dto.getTeacherName().trim().equals("Не указан") ? "Не указан" : teacherName[0]),
                nullToEmpty(dto.getTeacherName().trim().equals("Не указан") ? " " : teacherName[2]),
                nullToEmpty(dto.getTeacherName().trim().equals("Не указан") ? " ": teacherName[1]),
                nullToEmpty(dto.getClassroom()),
                nullToEmpty(dto.getLessonType())
        );
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
