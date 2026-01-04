package ru.semavin.telegrambot.services.schedules;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ScheduleMergeServiceTest {

    @InjectMocks
    private ScheduleMergingService scheduleService;

    @Mock
    private SemesterService semesterService;

    @Mock
    private GroupService groupService;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private ScheduleChangeService scheduleChangeService;

    private final String GROUP_NAME = "М3О-203С-22";

    private final LocalDate TEST_DATE = LocalDate.of(2025, 12, 12);

    private final GroupEntity GROUP_ENTITY = GroupEntity.builder()
            .groupName(GROUP_NAME)
            .build();

    private final String TEST_ROOM_1 = "3-321";
    private final String TEST_ROOM_2 = "3-123";

    private final String TEST_SUB_1 = "Предмет_1";
    private final String TEST_SUB_2 = "Предмет_2";

    private final LocalTime START_TIME_1 = LocalTime.of(10, 23);
    private final LocalTime START_TIME_2 = LocalTime.of(10, 33);

    private final LocalTime END_TIME_1 = LocalTime.of(13, 23);
    private final LocalTime END_TIME_2 = LocalTime.of(13, 33);

    private final String CONTROL_SUM_1 = "sum_1";
    private final String CONTROL_SUM_2 = "sum_2";

    @Test
    @DisplayName("Если есть переносы пар, то они должны быть добавлены в текущий")
    void success_getScheduleForDayWithPostponed() {
        List<ScheduleDTO> scheduleEntities = buildScheduleList();

        val changes = List.of(
                ScheduleChangeEntity.builder()
                        .newLessonDate(TEST_DATE)
                        .newStartTime(START_TIME_1.plusMinutes(100))
                        .group(GROUP_ENTITY)
                        .build()
        );

        List<ScheduleDTO> result = scheduleService
                .mergeChanges(scheduleEntities, changes, TEST_DATE);

        assertEquals(3, result.size());
    }

    @Test
    @DisplayName("Если нет переносов пар, то просто меняем пары для текущего")
    void success_getScheduleForDayWithoutPostponed() {
        List<ScheduleDTO> scheduleEntities = buildScheduleList();

        val newStartTime = START_TIME_1.plusMinutes(100);
        val newEndTime = END_TIME_1.plusMinutes(100);
        val description = "123";
        val newTestRoom = "3-234324";

        val changes = List.of(
                ScheduleChangeEntity.builder()
                        .oldLessonDate(TEST_DATE)
                        .newStartTime(newStartTime)
                        .newEndTime(newEndTime)
                        .description(description)
                        .classroom(newTestRoom)
                        .group(GROUP_ENTITY)
                        .oldControlSum(CONTROL_SUM_1)
                        .build()
        );

        List<ScheduleDTO> result = scheduleService
                .mergeChanges(scheduleEntities, changes, TEST_DATE);

        assertEquals(2, result.size());
        assertThat(result).anySatisfy(lesson -> {
            assertThat(lesson.getControlSum()).isEqualTo(CONTROL_SUM_1);
            assertThat(lesson.getStartTime()).isEqualTo(newStartTime);
            assertThat(lesson.getEndTime()).isEqualTo(newEndTime);
            assertThat(lesson.getDescription()).isEqualTo(description);
            assertThat(lesson.getClassroom()).isEqualTo(newTestRoom);
        });
    }

    private List<ScheduleDTO> buildScheduleList() {
        return List.of(
                ScheduleDTO.builder()
                        .lessonDate(TEST_DATE)
                        .groupName(GROUP_NAME)
                        .classroom(TEST_ROOM_1)
                        .startTime(START_TIME_1)
                        .endTime(END_TIME_1)
                        .subjectName(TEST_SUB_1)
                        .lessonType(LessonType.PRACTICAL.name())
                        .controlSum(CONTROL_SUM_1)
                        .build(),
                ScheduleDTO.builder()
                        .lessonDate(TEST_DATE)
                        .groupName(GROUP_NAME)
                        .classroom(TEST_ROOM_2)
                        .startTime(START_TIME_2)
                        .endTime(END_TIME_2)
                        .subjectName(TEST_SUB_2)
                        .lessonType(LessonType.PRACTICAL.name())
                        .controlSum(CONTROL_SUM_2)
                        .build()
        );
    }


}
