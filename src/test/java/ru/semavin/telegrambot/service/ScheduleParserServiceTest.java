package ru.semavin.telegrambot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.schedules.ScheduleParserService;
import ru.semavin.telegrambot.services.schedules.SemesterService;


import static org.mockito.Mockito.when;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private ObjectMapper mapper;
    // Используем реальный SemesterService с датой начала семестра
    private SemesterService semesterService = new SemesterService("01.09.2025", "26.01.2026");
    private SemesterService semesterServiceLater = new SemesterService("10.10.2025", "26.01.2026");
    private ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    private UserEntity teacherNonEmpty;
    private UserEntity teacherEmpty;
    private UserEntity teacherSAU;
    private UserEntity teacherNavigation;
    private UserEntity teacherManager;
    private UserEntity teacherRadio;
    private UserEntity teacherGiro;
    private GroupEntity groupEntity;

    @InjectMocks
    private ScheduleParserService scheduleParserService;
    @InjectMocks
    private ScheduleParserService scheduleParserServiceWithSemesterStartAfterLessons;
    private String fakeJsonSuccessForAnyOne;
    private String fakeJsonSuccessForEmpty;
    private String fakeJsonSuccessWithoutDoublePairs;
    private String fakeJsonSuccessWithDoublePairs;
    private String FIRSTPAIR_START = "09:00";
    private String FIRSTPAIR_END = "10:30";
    private String SECONDPAIR_START = "10:45";
    private String SECONDPAIR_END = "12:15";
    private String THIRDPAIR_START = "13:00";
    private String THIRDPAIR_END = "14:30";
    private String FOURTHPAIR_START = "14:45";
    private String FOURTHPAIR_END = "16:15";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void setUp() {
        fakeJsonSuccessWithDoublePairs = """
                {
                  "08.09.2025": {
                    "day": "Пн",
                    "pairs": {
                      "9:00:00": {
                        "Системы автоматического управления воздушными летательными аппаратами": {
                          "time_start": "9:00:00",
                          "time_end": "10:30:00",
                          "lector": {
                            "578c176a-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛР": 1
                          },
                          "room": {
                            "a3d1acc2-02fb-11e0-bf99-003048ccec9b": "3-141"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "10:45:00": {
                        "Системы автоматического управления воздушными летательными аппаратами": {
                          "time_start": "10:45:00",
                          "time_end": "12:15:00",
                          "lector": {
                            "578c176a-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛР": 1
                          },
                          "room": {
                            "a3d1acc2-02fb-11e0-bf99-003048ccec9b": "3-141"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "13:00:00": {
                        "Основы менеджмента": {
                          "time_start": "13:00:00",
                          "time_end": "14:30:00",
                          "lector": {
                            "03d272fd-3eb4-11eb-9812-485b3919ee6d": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "638ee0da-7664-11e0-a630-003048ccec9b": "5-235"
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

        fakeJsonSuccessWithoutDoublePairs = """
                {
                "05.09.2025": {
                    "day": "Пт",
                    "pairs": {
                      "9:00:00": {
                        "Радионавигационные системы": {
                          "time_start": "9:00:00",
                          "time_end": "10:30:00",
                          "lector": {
                            "578c17f8-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛК": 1
                          },
                          "room": {
                            "17119f5f-07b5-11e0-bf99-003048ccec9b": "3-316"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "10:45:00": {
                        "Гироскопические стабилизаторы": {
                          "time_start": "10:45:00",
                          "time_end": "12:15:00",
                          "lector": {
                            "2f38b9d1-1d9b-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛК": 1
                          },
                          "room": {
                            "c493ff2a-07bb-11e0-bf99-003048ccec9b": "3-404"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "13:00:00": {
                        "Основы теории пилотажно-навигационных систем": {
                          "time_start": "13:00:00",
                          "time_end": "14:30:00",
                          "lector": {
                            "f253618c-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ЛК": 1
                          },
                          "room": {
                            "8153f489-0ba2-11e0-bf99-003048ccec9b": "ГУК Б-649"
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "14:45:00": {
                        "Основы теории пилотажно-навигационных систем": {
                          "time_start": "14:45:00",
                          "time_end": "16:15:00",
                          "lector": {
                            "f253618c-1d99-11e0-9baf-1c6f65450efa": "Иванов Иван Иванович"
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "8153f489-0ba2-11e0-bf99-003048ccec9b": "ГУК Б-649"
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

        fakeJsonSuccessForAnyOne = """
                {
                  "02.09.2025": {
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

        fakeJsonSuccessForEmpty = """
                {
                "04.09.2025": {
                    "day": "Чт",
                    "pairs": {
                      "9:00:00": {
                        "Военная подготовка": {
                          "time_start": "9:00:00",
                          "time_end": "10:30:00",
                          "lector": {
                            "00000000-0000-0000-0000-000000000000": ""
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "b74722b0-6af3-11e6-ba19-003048dec27f": "--каф."
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "10:45:00": {
                        "Военная подготовка": {
                          "time_start": "10:45:00",
                          "time_end": "12:15:00",
                          "lector": {
                            "00000000-0000-0000-0000-000000000000": ""
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "b74722b0-6af3-11e6-ba19-003048dec27f": "--каф."
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "13:00:00": {
                        "Военная подготовка": {
                          "time_start": "13:00:00",
                          "time_end": "14:30:00",
                          "lector": {
                            "00000000-0000-0000-0000-000000000000": ""
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "b74722b0-6af3-11e6-ba19-003048dec27f": "--каф."
                          },
                          "lms": "",
                          "teams": "",
                          "other": ""
                        }
                      },
                      "14:45:00": {
                        "Военная подготовка": {
                          "time_start": "14:45:00",
                          "time_end": "16:15:00",
                          "lector": {
                            "00000000-0000-0000-0000-000000000000": ""
                          },
                          "type": {
                            "ПЗ": 1
                          },
                          "room": {
                            "b74722b0-6af3-11e6-ba19-003048dec27f": "--каф."
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
        scheduleParserService = new ScheduleParserService(semesterService, teacherService, restTemplate, mapper, executorService);
        scheduleParserServiceWithSemesterStartAfterLessons = new ScheduleParserService(semesterServiceLater, teacherService, restTemplate, mapper, executorService);
        // Заменяем restTemplate внутри ScheduleParserService на наш мок
        ReflectionTestUtils.setField(scheduleParserService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(scheduleParserServiceWithSemesterStartAfterLessons, "restTemplate", restTemplate);

        teacherNonEmpty = UserEntity.builder()
                .teacherUuid("578c176d-1d99-11e0-9baf-1c6f65450efa")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherSAU = UserEntity.builder()
                .teacherUuid("578c176a-1d99-11e0-9baf-1c6f65450efa")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherManager = UserEntity.builder()
                .teacherUuid("03d272fd-3eb4-11eb-9812-485b3919ee6d")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherRadio = UserEntity.builder()
                .teacherUuid("578c17f8-1d99-11e0-9baf-1c6f65450efa")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherNavigation = UserEntity.builder()
                .teacherUuid("f253618c-1d99-11e0-9baf-1c6f65450efa")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherGiro = UserEntity.builder()
                .teacherUuid("2f38b9d1-1d9b-11e0-9baf-1c6f65450efa")
                .firstName("Иван")
                .lastName("Иванов")
                .patronymic("Иванович")
                .role(UserRole.TEACHER)
                .build();

        teacherEmpty = UserEntity.builder()
                .teacherUuid("00000000-0000-0000-0000-000000000000")
                .firstName("Не указан")
                .lastName(" ")
                .patronymic(" ")
                .role(UserRole.TEACHER)
                .build();

        groupEntity = GroupEntity.builder().groupName("М3О-403С-22").build();

    }

    @Test
    void testParseJsonScheduleWithEmptyLector() throws Exception {
        when(mapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(fakeJsonSuccessForEmpty));
        // Мокаем вызов restTemplate.getForObject() для любого URL, чтобы возвращать fakeJson
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(fakeJsonSuccessForEmpty);

        when(teacherService.findOrCreateTeacherAndAddGroup("00000000-0000-0000-0000-000000000000", "", groupEntity))
                .thenReturn(teacherEmpty);

        List<ScheduleEntity> result = scheduleParserService.findScheduleByGroup(groupEntity);
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Список занятий не должен быть пустым");
        assertEquals(2, result.size(), "Должна быть 2 записи расписания");

        // Проверяем первую пару (9:00:00)
        ScheduleEntity firstPair = result.get(0);
        ScheduleEntity secondPair = result.get(1);
        assertEquals("Военная подготовка", firstPair.getSubjectName());
        assertEquals(LessonType.PRACTICAL, firstPair.getLessonType());
        // Преподаватель должен быть "Не указан"
        assertNotNull(firstPair.getTeacher());
        assertEquals("Не указан", firstPair.getTeacher().getFirstName());
        // Аудитория
        assertEquals("--каф.", firstPair.getClassroom());
        // Время начала и окончания
        assertEquals("09:00", firstPair.getStartTime().toString());
        assertEquals("12:15", firstPair.getEndTime().toString());

        assertEquals("13:00", secondPair.getStartTime().toString());
        assertEquals("16:15", secondPair.getEndTime().toString());
    }

    @Test
    void testParseJsonScheduleWithNonEmptyLector() throws Exception {
        when(mapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(fakeJsonSuccessForAnyOne));
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(fakeJsonSuccessForAnyOne);


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
        assertEquals("02.09.2025", formattedDate);
        assertEquals("13:00", secondPair.getStartTime().toString());
        assertEquals("14:30", secondPair.getEndTime().toString());
    }

    @Test
    void testParseJsonScheduleDouble() throws Exception {
        when(mapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(fakeJsonSuccessWithDoublePairs));
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(fakeJsonSuccessWithDoublePairs);

        when(teacherService.findOrCreateTeacherAndAddGroup("578c176a-1d99-11e0-9baf-1c6f65450efa", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherSAU);

        when(teacherService.findOrCreateTeacherAndAddGroup("03d272fd-3eb4-11eb-9812-485b3919ee6d", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherManager);

        List<ScheduleEntity> result = scheduleParserService.findScheduleByGroup(groupEntity);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(2, result.size(), "Должно быть 2 записи расписания");
        ScheduleEntity secondPair = result.get(1);
        ScheduleEntity firstPair = result.get(0);

        String date = "08.09.2025";

        assertEqualsPairMeta(firstPair,
                "Системы автоматического управления воздушными летательными аппаратами",
                LessonType.LAB,
                teacherSAU.getTeacherUuid(),
                date,
                FIRSTPAIR_START,
                SECONDPAIR_END);

        assertEqualsPairMeta(secondPair,
                "Основы менеджмента",
                LessonType.PRACTICAL,
                teacherManager.getTeacherUuid(),
                date,
                THIRDPAIR_START,
                THIRDPAIR_END);
    }

    @Test
    void testParseJsonSchedule() throws Exception {
        when(mapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(fakeJsonSuccessWithoutDoublePairs));
        when(restTemplate.getForObject(anyString(), eq(String.class)))
                .thenReturn(fakeJsonSuccessWithoutDoublePairs);

        when(teacherService.findOrCreateTeacherAndAddGroup("f253618c-1d99-11e0-9baf-1c6f65450efa", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherNavigation);

        when(teacherService.findOrCreateTeacherAndAddGroup("578c17f8-1d99-11e0-9baf-1c6f65450efa", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherRadio);

        when(teacherService.findOrCreateTeacherAndAddGroup("2f38b9d1-1d9b-11e0-9baf-1c6f65450efa", "Иванов Иван Иванович", groupEntity))
                .thenReturn(teacherGiro);

        List<ScheduleEntity> result = scheduleParserService.findScheduleByGroup(groupEntity);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(4, result.size(), "Должно быть 4 записи расписания");
        ScheduleEntity secondPair = result.get(1);
        ScheduleEntity firstPair = result.get(0);
        ScheduleEntity thirdPair = result.get(2);
        ScheduleEntity fourthPair = result.get(3);


        String date = "05.09.2025";
        //первая
        assertEqualsPairMeta(firstPair,
                "Радионавигационные системы",
                LessonType.LECTURE,
                teacherRadio.getTeacherUuid(),
                date,
                FIRSTPAIR_START,
                FIRSTPAIR_END);

        assertEqualsPairMeta(secondPair,
                "Гироскопические стабилизаторы",
                LessonType.LECTURE,
                teacherGiro.getTeacherUuid(),
                date,
                SECONDPAIR_START,
                SECONDPAIR_END);

        assertEqualsPairMeta(thirdPair,
                "Основы теории пилотажно-навигационных систем",
                LessonType.LECTURE,
                teacherNavigation.getTeacherUuid(),
                date,
                THIRDPAIR_START,
                THIRDPAIR_END);

        assertEqualsPairMeta(fourthPair,
                "Основы теории пилотажно-навигационных систем",
                LessonType.PRACTICAL,
                teacherNavigation.getTeacherUuid(),
                date,
                FOURTHPAIR_START,
                FOURTHPAIR_END);
    }

    private void assertEqualsPairMeta(ScheduleEntity secondPair,
                                      String subjName,
                                      LessonType lessonType,
                                      String UUID,
                                      String date,
                                      String needStartTime,
                                      String needEndTime) {
        //Вторая
        assertEquals(subjName, secondPair.getSubjectName());
        assertEquals(lessonType, secondPair.getLessonType());
        assertEquals(UUID, secondPair.getTeacher().getTeacherUuid());
        String formattedDateSecond = secondPair.getLessonDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        assertEquals(date, formattedDateSecond);
        assertEquals(needStartTime, secondPair.getStartTime().toString());
        assertEquals(needEndTime, secondPair.getEndTime().toString());
    }

    @Test
    void testParseJsonScheduleWhenSemesterStartBeforeDateLesson() throws Exception {
        when(mapper.readTree(anyString())).thenReturn(new ObjectMapper().readTree(fakeJsonSuccessForAnyOne));
        when(restTemplate.getForObject(anyString(), eq(String.class))).thenReturn(fakeJsonSuccessForAnyOne);

        List<ScheduleEntity> result = scheduleParserServiceWithSemesterStartAfterLessons.findScheduleByGroup(groupEntity);

        assertTrue(result.isEmpty());
    }
}
