package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.LessonType;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleSeriesIdServiceTest {
    @Test
    void weeklyOccurrencesHaveSameSeriesIdEvenWhenClassroomVaries() {
        ScheduleEntity first = lesson(LocalDate.of(2026, 9, 1), "101", "teacher-1", "Math");
        ScheduleEntity nextWeek = lesson(LocalDate.of(2026, 9, 8), "202", "teacher-1", "Math");

        assertThat(ScheduleSeriesIdService.calculate(first))
                .isEqualTo(ScheduleSeriesIdService.calculate(nextWeek));
    }

    @Test
    void differentTeacherOrSemesterProducesDifferentSeries() {
        ScheduleEntity autumn = lesson(LocalDate.of(2026, 9, 1), "101", "teacher-1", "Math");
        ScheduleEntity anotherTeacher = lesson(LocalDate.of(2026, 9, 8), "101", "teacher-2", "Math");
        ScheduleEntity nextSemester = lesson(LocalDate.of(2027, 2, 2), "101", "teacher-1", "Math");

        assertThat(ScheduleSeriesIdService.calculate(autumn))
                .isNotEqualTo(ScheduleSeriesIdService.calculate(anotherTeacher))
                .isNotEqualTo(ScheduleSeriesIdService.calculate(nextSemester));
    }

    @Test
    void missingTeacherHasStableExplicitSeriesIdentity() {
        ScheduleEntity withoutTeacher = lesson(
                LocalDate.of(2026, 9, 1), "101", "teacher-1", "Math");
        withoutTeacher.setTeacher(null);
        ScheduleEntity anotherOccurrence = lesson(
                LocalDate.of(2026, 9, 8), "202", "teacher-1", "Math");
        anotherOccurrence.setTeacher(null);

        assertThat(ScheduleSeriesIdService.calculate(withoutTeacher))
                .isEqualTo(ScheduleSeriesIdService.calculate(anotherOccurrence));
    }

    private ScheduleEntity lesson(LocalDate date, String room, String teacherUuid, String subject) {
        return ScheduleEntity.builder()
                .group(GroupEntity.builder().groupName("GROUP-1").build())
                .subjectName(subject)
                .lessonType(LessonType.LECTURE)
                .teacher(UserEntity.builder().teacherUuid(teacherUuid).build())
                .classroom(room)
                .lessonDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .build();
    }
}
