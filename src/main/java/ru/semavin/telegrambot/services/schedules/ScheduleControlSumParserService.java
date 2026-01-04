package ru.semavin.telegrambot.services.schedules;

import lombok.val;
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
