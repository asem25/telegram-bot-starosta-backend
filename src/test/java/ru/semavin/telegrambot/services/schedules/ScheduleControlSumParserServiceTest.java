package ru.semavin.telegrambot.services.schedules;

import lombok.val;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.utils.DateUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleControlSumParserServiceTest {

    @Test
    void success_fillCalculateSumDate() {
        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 11, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumTime() {
        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(14, 0),
                        LocalTime.of(16, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumFirstNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName2",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumLastNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName2",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumPatronymicNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")

        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumClassRoomNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-243",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-242",
                        "LECTURE",
                        "group1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumLessonTypeNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-243",
                        "LECTURE",
                        "gr1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-243",
                        "PRACTICAL",
                        "gr1",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumGroupsNames() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-243",
                        "PRACTICAL",
                        "group1",
                        "sub1")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic2",
                        "3-243",
                        "PRACTICAL",
                        "group2",
                        "sub1")
        );

        assertNotEquals(hash1, hash2);
    }

    @Test
    void success_fillCalculateSumSubject() {

        val hash1 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-243",
                        "PRACTICAL",
                        "group1",
                        "subject2")
        );

        val hash2 = ScheduleControlSumParserService.fillCalculateSum(
                buildSchedule(LocalDate.of(2025, 10, 20),
                        LocalTime.of(13, 0),
                        LocalTime.of(14, 30),
                        "firstName1",
                        "lastName1",
                        "patronymic1",
                        "3-243",
                        "PRACTICAL",
                        "group1",
                        "subject1")
        );

        assertNotEquals(hash1, hash2);
    }

    private ScheduleEntity buildSchedule(LocalDate date,
                                         LocalTime time,
                                         LocalTime end,
                                         String firstName,
                                         String lastName,
                                         String patronymic,
                                         String classRoom,
                                         String lessonType,
                                         String group,
                                         String subject
    ) {
        return ScheduleEntity.builder()
                .group(GroupEntity.builder().groupName(group).build())
                .lessonDate(date)
                .lessonWeek(1)
                .subjectName(subject)
                .lessonType(LessonType.valueOf(lessonType))
                .classroom(classRoom)
                .teacher(UserEntity.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .patronymic(patronymic)
                        .build())
                .startTime(time)
                .endTime(end)
                .build();
    }
}