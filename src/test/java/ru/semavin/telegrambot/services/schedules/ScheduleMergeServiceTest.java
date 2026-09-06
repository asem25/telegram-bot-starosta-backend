package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleMergeServiceTest {

    private static final String GROUP = "М3О-203С-22";
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 10);
    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    private final ScheduleMergingService service =
            new ScheduleMergingService(null, null, null, null, null);

    @Test
    void returnsCopiesAndDoesNotMutateCachedInput() {
        ScheduleDTO source = lesson("sum-1", TODAY, 9, "Math", "Ivanov", "101", GROUP);

        List<ScheduleDTO> result = service.mergeChanges(List.of(source), List.of(
                change("sum-1", TODAY, null, false, 11)
        ), TODAY);

        assertThat(result).singleElement().satisfies(it -> assertThat(it.getStartTime()).isEqualTo(LocalTime.of(11, 0)));
        assertThat(source.getStartTime()).isEqualTo(LocalTime.of(9, 0));
    }

    @Test
    void processesAllChangesAfterAnOutboundMove() {
        List<ScheduleDTO> original = List.of(
                lesson("sum-1", TODAY, 9, "Math", "Ivanov", "101", GROUP),
                lesson("sum-2", TODAY, 11, "Physics", "Petrov", "102", GROUP)
        );
        List<ScheduleChangeEntity> changes = List.of(
                change("sum-1", TODAY, TOMORROW, false, 9),
                change("sum-2", TODAY, null, true, 11)
        );

        assertThat(service.mergeChanges(original, changes, TODAY)).isEmpty();
    }

    @Test
    void moveInsideSameDayReplacesOriginalInsteadOfDuplicatingIt() {
        ScheduleDTO original = lesson("sum-1", TODAY, 9, "Math", "Ivanov", "101", GROUP);

        List<ScheduleDTO> result = service.mergeChanges(List.of(original), List.of(
                change("sum-1", TODAY, TODAY, false, 13)
        ), TODAY);

        assertThat(result).singleElement().satisfies(it -> {
            assertThat(it.getStartTime()).isEqualTo(LocalTime.of(13, 0));
            assertThat(it.getControlSum()).isEqualTo("sum-1");
        });
    }

    @Test
    void addsLessonMovedFromAnotherDay() {
        ScheduleChangeEntity inbound = change("sum-1", TOMORROW, TODAY, false, 15);

        assertThat(service.mergeChanges(List.of(), List.of(inbound), TODAY))
                .singleElement()
                .satisfies(it -> {
                    assertThat(it.getLessonDate()).isEqualTo(TODAY);
                    assertThat(it.getStartTime()).isEqualTo(LocalTime.of(15, 0));
                    assertThat(it.getSubjectName()).isEqualTo("Changed subject");
                });
    }

    @Test
    void movedLessonKeepsOpaqueOccurrenceIdAndVersionOnTargetDate() {
        UUID occurrenceId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        ScheduleChangeEntity inbound = change("sum-1", TOMORROW, TODAY, false, 15);
        inbound.setOccurrenceId(occurrenceId);
        inbound.setSeriesId(seriesId);
        inbound.setVersion(3L);

        assertThat(service.mergeChanges(List.of(), List.of(inbound), TODAY))
                .singleElement()
                .satisfies(it -> {
                    assertThat(it.getLessonOccurrenceId()).isEqualTo(occurrenceId);
                    assertThat(it.getLessonSeriesId()).isEqualTo(seriesId);
                    assertThat(it.getVersion()).isEqualTo(3);
                });
    }

    @Test
    void ignoresChangeThatDoesNotBelongToRequestedDay() {
        ScheduleDTO original = lesson("sum-1", TODAY, 9, "Math", "Ivanov", "101", GROUP);
        ScheduleChangeEntity unrelated = change("sum-2", TOMORROW, TOMORROW.plusDays(1), false, 15);

        assertThat(service.mergeChanges(List.of(original), List.of(unrelated), TODAY))
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(original);
    }

    @Test
    void mergesOnlyTrulyIdenticalLessonsAcrossGroups() {
        Map<String, List<ScheduleDTO>> schedules = new LinkedHashMap<>();
        schedules.put("A", List.of(
                lesson("a1", TODAY, 9, "Math", "Ivanov", "101", "A"),
                lesson("a2", TODAY, 11, "Physics", "Petrov", "201", "A")
        ));
        schedules.put("B", List.of(
                lesson("b1", TODAY, 9, "Math", "Ivanov", "101", "B"),
                lesson("b2", TODAY, 11, "Physics", "Sidorov", "202", "B")
        ));

        List<ScheduleDTO> result = service.mergeMultiGroups(schedules);

        assertThat(result).hasSize(3);
        assertThat(result).filteredOn(it -> it.getStartTime().equals(LocalTime.of(9, 0)))
                .singleElement().extracting(ScheduleDTO::getGroupName).isEqualTo("A, B");
        assertThat(result).filteredOn(it -> it.getStartTime().equals(LocalTime.of(11, 0)))
                .extracting(ScheduleDTO::getGroupName).containsExactlyInAnyOrder("A", "B");
    }

    private ScheduleChangeEntity change(String sum, LocalDate oldDate, LocalDate newDate,
                                        boolean deleted, int newHour) {
        return ScheduleChangeEntity.builder()
                .oldControlSum(sum)
                .oldLessonDate(oldDate)
                .newLessonDate(newDate)
                .newStartTime(LocalTime.of(newHour, 0))
                .newEndTime(LocalTime.of(newHour + 1, 30))
                .subjectName("Changed subject")
                .lessonType("ЛК")
                .teacherName("Changed teacher")
                .classroom("303")
                .group(GroupEntity.builder().groupName(GROUP).build())
                .deleted(deleted)
                .build();
    }

    private ScheduleDTO lesson(String sum, LocalDate date, int hour, String subject,
                               String teacher, String room, String group) {
        return ScheduleDTO.builder()
                .controlSum(sum)
                .lessonDate(date)
                .startTime(LocalTime.of(hour, 0))
                .endTime(LocalTime.of(hour + 1, 30))
                .subjectName(subject)
                .lessonType("ЛК")
                .teacherName(teacher)
                .classroom(room)
                .groupName(group)
                .build();
    }
}
