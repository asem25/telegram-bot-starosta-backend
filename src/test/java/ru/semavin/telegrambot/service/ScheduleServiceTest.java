package ru.semavin.telegrambot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.services.schedules.ScheduleParserService;
import ru.semavin.telegrambot.services.schedules.ScheduleService;
import ru.semavin.telegrambot.services.schedules.SemesterService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private ScheduleParserService scheduleParserService;

    @Mock
    private ScheduleMapper scheduleMapper;

    @Mock
    private SemesterService semesterService;

    @Mock
    private GroupService groupService;

    @InjectMocks
    private ScheduleService scheduleService;


    /**
     * Метод инициализации данных перед каждым тестом.
     * Здесь можно заполнить нужные моки, если нужно задать общие stub-значения.
     */
    @BeforeEach
    void setUp() {
    }
    @Test
    void testGetScheduleFromDataBase() {
        // Примерная структура:
        // 1. Настраиваем моки (when(...) thenReturn(...))
        GroupEntity mockGroup = new GroupEntity();
        mockGroup.setGroupName("М3О-303С-22");

        when(groupService.findEntityByName(anyString()))
                .thenReturn(mockGroup);

        // Допустим, у нас есть несколько ScheduleEntity в БД
        ScheduleEntity scheduleEntity = new ScheduleEntity();
        scheduleEntity.setGroup(mockGroup);

        when(scheduleRepository.findAllByGroupAndLessonWeek(any(GroupEntity.class), anyInt()))
                .thenReturn(Collections.singletonList(scheduleEntity));

        // Чтобы собрать DTO
        ScheduleDTO scheduleDTO = ScheduleDTO.builder().build();
        when(scheduleMapper.toScheduleDTOList(anyList()))
                .thenReturn(Collections.singletonList(scheduleDTO));


        // 4. Убедимся, что метод вызывался ровно 1 раз
        verify(scheduleRepository, times(1)).findAllByGroupAndLessonWeek(mockGroup, 3);
    }

    /**
     * Шаблон теста для проверки логики получения актуального расписания:
     * {@link ScheduleService#getActualSchedule(String)} .
     * TODO: дополните логику теста нужными проверками.
     */
    @Test
    void testGetActualSchedule() {
        // 1. Настраиваем моки
        GroupEntity mockGroup = new GroupEntity();
        mockGroup.setGroupName("М3О-303С-22");

        when(groupService.findEntityByName("М3О-303С-22"))
                .thenReturn(mockGroup);

        // Имитируем, что парсер вернёт некий список ScheduleEntity
        ScheduleEntity parsedEntity = new ScheduleEntity();
        parsedEntity.setGroup(mockGroup);
        parsedEntity.setSubjectName("Test Subject");

        when(scheduleParserService.findScheduleByGroup(any(GroupEntity.class)))
                .thenReturn(Collections.singletonList(parsedEntity));

        // Мокируем маппер
        ScheduleDTO scheduleDTO = ScheduleDTO.builder().build();
        scheduleDTO.setSubjectName("Test Subject DTO");
        when(scheduleMapper.toScheduleDTOList(anyList()))
                .thenReturn(Collections.singletonList(scheduleDTO));

        // 2. Вызываем метод, который должен обратиться к парсеру и сохранить данные
        List<ScheduleDTO> result = scheduleService.getActualSchedule("М3О-303С-22");

//        // 3. Проверяем полученные результаты
        assertNotNull(result, "Список DTO не должен быть null");
        assertFalse(result.isEmpty(), "Список DTO не должен быть пустым");
        assertEquals("Test Subject DTO", result.get(0).getSubjectName(), "Неверный предмет в DTO");

        // 4. Проверяем, что scheduleRepository сохранит полученные данные
        verify(scheduleRepository, times(1)).deleteAllByGroup(any(GroupEntity.class));
        verify(scheduleRepository, times(1)).saveAllAndFlush(anyList());
    }

    /**
     * Шаблон теста для проверки получения расписания за конкретный день:
     * {@link ScheduleService#getScheduleForDay(String, String)}.
     * TODO: дополните логику теста нужными проверками.
     */
    @Test
    void testGetScheduleForDay() {
        // 1. Настраиваем моки
        GroupEntity mockGroup = new GroupEntity();
        mockGroup.setGroupName("М3О-303С-22");
        when(groupService.findEntityByName("М3О-303С-22"))
                .thenReturn(mockGroup);

        // Пусть наш SemesterService вернёт корректную неделю для указанной даты
        when(semesterService.getWeekForDate("21.03.2025"))
                .thenReturn("3");

        // Проверка, есть ли расписание на конкретный день:
        when(scheduleRepository.existsByLessonDateAndGroup(any(), eq(mockGroup)))
                .thenReturn(true);

        // Возврат результатов
        ScheduleEntity entity = new ScheduleEntity();
        entity.setGroup(mockGroup);
        entity.setSubjectName("Some Subject");
        when(scheduleRepository.findAllByLessonDateAndGroup(any(), eq(mockGroup)))
                .thenReturn(Collections.singletonList(entity));

        // Маппер
        ScheduleDTO dto = ScheduleDTO.builder().build();
        dto.setSubjectName("Some Subject DTO");
        when(scheduleMapper.toScheduleDTOList(anyList()))
                .thenReturn(Collections.singletonList(dto));

        // 2. Вызываем метод
        List<ScheduleDTO> result = scheduleService.getScheduleForDay("М3О-303С-22", "21.03.2025");

//        // 3. Проверяем, что вернулся корректный список
        assertNotNull(result, "Результат не должен быть null");
        assertFalse(result.isEmpty(), "Список не должен быть пустым");
        assertEquals("Some Subject DTO", result.get(0).getSubjectName(), "Название предмета не совпадает");

        // 4. Проверяем, что repository был вызван 1 раз
        verify(scheduleRepository, times(1)).existsByLessonDateAndGroup(any(), eq(mockGroup));
        verify(scheduleRepository, times(1)).findAllByLessonDateAndGroup(any(), eq(mockGroup));
    }
}
