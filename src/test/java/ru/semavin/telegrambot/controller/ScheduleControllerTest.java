package ru.semavin.telegrambot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.semavin.telegrambot.services.schedules.SchedulerCalendarISCService;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleControllerTest {

    @Test
    void exposesOnlyGroupAndTeacherCalendarFeeds() {
        Map<String, String> routes = Arrays.stream(ScheduleController.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(GetMapping.class))
                .collect(Collectors.toMap(Method::getName,
                        method -> method.getAnnotation(GetMapping.class).value()[0]));

        assertEquals(Map.of(
                "getSemesterScheduleFeed", "/semester/feed",
                "getTeacherSemesterScheduleFeed", "/teacher/semester/feed"
        ), routes);
    }

    @Test
    void dependsOnlyOnCalendarFeedService() {
        Field[] fields = Arrays.stream(ScheduleController.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toArray(Field[]::new);

        assertEquals(1, fields.length);
        assertEquals(SchedulerCalendarISCService.class, fields[0].getType());
    }

    @Test
    void lastUpdateBadgeFlagDefaultsToFalse() throws NoSuchMethodException {
        Method method = ScheduleController.class.getDeclaredMethod(
                "getSemesterScheduleFeed", String.class, boolean.class);
        RequestParam annotation = method.getParameters()[1].getAnnotation(RequestParam.class);

        assertEquals("false", annotation.defaultValue());
    }

    @Test
    void passesLastUpdateBadgeFlagToCalendarService() {
        SchedulerCalendarISCService calendarService = mock(SchedulerCalendarISCService.class);
        when(calendarService.getIscCalendarByGroupName("GROUP-1", true)).thenReturn("calendar");
        ScheduleController controller = new ScheduleController(calendarService);

        assertEquals("calendar", controller.getSemesterScheduleFeed("GROUP-1", true).getBody());
        verify(calendarService).getIscCalendarByGroupName("GROUP-1", true);
    }
}
