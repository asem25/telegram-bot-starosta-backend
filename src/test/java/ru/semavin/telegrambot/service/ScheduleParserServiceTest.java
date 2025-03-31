package ru.semavin.telegrambot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.services.GroupService;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.schedules.ScheduleParserService;
import ru.semavin.telegrambot.services.schedules.SemesterService;

import static org.mockito.Mockito.when;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class ScheduleParserServiceTest {
    @Mock
    private UserService teacherService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private GroupService groupService; // если понадобится

    // Используем реальный SemesterService с датой начала семестра
    private SemesterService semesterService = new SemesterService("01.09.2024");
    private SemesterService semesterServiceLater = new SemesterService("10.10.2024");
    private UserEntity teacherNonEmpty;
    private UserEntity teacherEmpty;
    private GroupEntity groupEntity;


    private ScheduleParserService scheduleParserService;
    private ScheduleParserService scheduleParserServiceWithSemesterStartAfterLessons;
    private String fakeJson;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        fakeJson = """
                {
                  "02.09.2024": {
                    "day": "Пн",
                    "pairs": {
                      "9:00:00": {
                        "Физическая культура (спортивные секции)": {
                          "time_start": "9:00:00",
                          "time_end": "10:30:00",
                          "lector": {
                            "00000000-0000-0000-0000-000000000000": ""
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "9bd25e78-5fc5-11ef-bc20-3cecef1c132f": "--каф. 919"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "13:00:00": {
                        "Основы теории управления": {
                          "time_start": "13:00:00",
                          "time_end": "14:30:00",
                          "lector": {
                            "578c176d-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛК": 1
                          },
                          "room": {
                            "ec99efca-0302-11e0-bf99-003048ccec9b": "3-Зал А"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      }
                    }
                  }
                }
                """;
        scheduleParserService = new ScheduleParserService(semesterService, teacherService, groupService);
        scheduleParserServiceWithSemesterStartAfterLessons = new ScheduleParserService(semesterServiceLater, teacherService, groupService);
        // Заменяем restTemplate внутри ScheduleParserService на наш мок
        ReflectionTestUtils.setField(scheduleParserService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(scheduleParserServiceWithSemesterStartAfterLessons, "restTemplate", restTemplate);

        teacherNonEmpty = UserEntity.builder()
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();
        ;

        teacherEmpty = UserEntity.builder()
                .firstName("Не указан")
                .role(UserRole.TEACHER)
                .build();

        groupEntity = GroupEntity.builder().groupName("М3О-303С-22").build();

    }

    @Test
    void testParseJsonScheduleWithEmptyLector() throws Exception {

        // Мокаем вызов restTemplate.getForObject() для любого URL, чтобы возвращать fakeJson
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(fakeJson);

        // Stub для teacherService: для пустого лектора возвращаем преподавателя с именем "Не указан"
        UserEntity teacherEmpty = new UserEntity();
        teacherEmpty.setFirstName("Не указан");
        teacherEmpty.setRole(UserRole.TEACHER);
        when(teacherService.findOrCreateTeacherAndAddGroup("00000000-0000-0000-0000-000000000000", "", groupEntity))
                .thenReturn(teacherEmpty);


        List<ScheduleEntity> result = scheduleParserService.findScheduleByGroup(groupEntity);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Список занятий не должен быть пустым");
        assertEquals(2, result.size(), "Должно быть 2 записи расписания");

        // Проверяем первую пару (9:00:00)
        ScheduleEntity firstPair = result.get(0);
        // Ожидаем предмет "Физическая культура (спортивные секции)"
        assertEquals("Физическая культура (спортивные секции)", firstPair.getSubjectName());
        assertEquals(LessonType.PRACTICAL, firstPair.getLessonType());
        // Преподаватель должен быть "Не указан"
        assertNotNull(firstPair.getTeacher());
        assertEquals("Не указан", firstPair.getTeacher().getFirstName());
        // Аудитория
        assertEquals("--каф. 919", firstPair.getClassroom());
        // Дата должна быть 02.09.2024
        String formattedDate = firstPair.getLessonDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertEquals("02.09.2024", formattedDate);
        // Время начала и окончания
        assertEquals("09:00", firstPair.getStartTime().toString());
        assertEquals("10:30", firstPair.getEndTime().toString());
    }

    @Test
    void testParseJsonScheduleWithNonEmptyLector() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(fakeJson);


        when(teacherService.findOrCreateTeacherAndAddGroup("00000000-0000-0000-0000-000000000000", "", groupEntity))
                .thenReturn(teacherEmpty);


        when(teacherService.findOrCreateTeacherAndAddGroup("578c176d-1d99-11e0-9baf-1c6f65450efa", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherNonEmpty);

        List<ScheduleEntity> result = scheduleParserService.findScheduleByGroup(groupEntity);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size(), "Должно быть 2 записи расписания");

        // Проверяем вторую пару (13:00:00)
        ScheduleEntity secondPair = result.get(1);
        log.info(result.toString());
        assertEquals("Основы теории управления", secondPair.getSubjectName());
        assertEquals(LessonType.LECTURE, secondPair.getLessonType());
        assertNotNull(secondPair.getTeacher());
        String teacherFullName = secondPair.getTeacher().getFirstName() + " " + secondPair.getTeacher().getPatronymic() + " " +
                secondPair.getTeacher().getLastName();
        assertEquals("Иван Иванович Иванов", teacherFullName.trim());
        assertEquals("3-Зал А", secondPair.getClassroom());
        String formattedDate = secondPair.getLessonDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertEquals("02.09.2024", formattedDate);
        assertEquals("13:00", secondPair.getStartTime().toString());
        assertEquals("14:30", secondPair.getEndTime().toString());
    }

    @Test
    void testParseJsonScheduleWhenSemesterStartBeforeDateLesson() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(fakeJson);

        List<ScheduleEntity> result = scheduleParserServiceWithSemesterStartAfterLessons.findScheduleByGroup(groupEntity);

        assertTrue(result.isEmpty());
    }
}
