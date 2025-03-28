package ru.semavin.telegrambot.services.schedules;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.ConnectFailedException;
import ru.semavin.telegrambot.utils.exceptions.GroupNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.InvalidFormatDateException;
import ru.semavin.telegrambot.utils.exceptions.ScheduleNotFoundException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Сервис для парсинга расписания занятий с сайта МАИ.
 */
@Slf4j
@Service

public class ScheduleParserService {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");
    private static final String SCHEDULE_URL = "https://mai.ru/education/studies/schedule/";
    private static final String GROUP_URL = "index.php?group=";
    private final int CURRENT_WEEK;

    public ScheduleParserService(SemesterService semesterService) {
        this.CURRENT_WEEK = Integer.parseInt(semesterService.getCurrentWeek());
    }

    /**
     * Главный метод: парсит расписание за любую неделю (или текущую, если week == null).
     */
    public List<ScheduleEntity> findScheduleByGroup(GroupEntity groupEntity, String week) {
        int actualWeek = week != null ? Integer.parseInt(week) : CURRENT_WEEK;
        log.info("Начало парсинга расписания для группы {}, неделя: {}", groupEntity.getGroupName(), (actualWeek));

        Map<String, String> cookies = getCookiesForGroup(groupEntity);
        Document schedulePage = getSchedulePage(groupEntity, cookies, actualWeek);

        // Парсим все дни (фильтр – любой день)
        return parseSchedule(schedulePage, groupEntity, date -> true, actualWeek);
    }

    /**
     * Получить куки для определённой группы.
     */
    private static Map<String, String> getCookiesForGroup(GroupEntity groupEntity)  {
        String url = SCHEDULE_URL + GROUP_URL + URLEncoder.encode(groupEntity.getGroupName(), StandardCharsets.UTF_8);

        Connection.Response response = null;
        try {
            response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer(SCHEDULE_URL)
                    .execute();
        } catch (IOException e) {
            log.error("Connect to {} failed", SCHEDULE_URL, e);
            throw ExceptionFabric.create(ConnectFailedException.class, ExceptionMessages.CONNECT_FAILED);
        }

        return response.cookies();
    }

    /**
     * Получить HTML-страницу с расписанием (учитываем week, если указана).
     */
    private Document getSchedulePage(GroupEntity groupEntity, Map<String, String> cookies, int week)  {
        String url = SCHEDULE_URL + GROUP_URL + URLEncoder.encode(groupEntity.getGroupName(), StandardCharsets.UTF_8);



        url += "&week=" + URLEncoder.encode(String.valueOf(week), StandardCharsets.UTF_8);
        log.debug("Запрашиваем страницу расписания: {}", url);

        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .referrer(SCHEDULE_URL)
                    .cookies(cookies)
                    .get();
        } catch (IOException e) {
            log.error("Connect to {} failed", SCHEDULE_URL, e);
            throw ExceptionFabric.create(ConnectFailedException.class, ExceptionMessages.CONNECT_FAILED);
        }
    }

    /**
     * Универсальный метод парсинга: берёт все дни, фильтрует их и парсит пары.
     *
     * @param doc       HTML-документ расписания
     * @param groupEntity Имя группы
     * @param dayFilter Условие, по которому отбираются дни (например, "только сегодня")
     * @return Список распарсенных пар
     */
    private List<ScheduleEntity> parseSchedule(Document doc, GroupEntity groupEntity, Predicate<LocalDate> dayFilter, int week) {
        List<ScheduleEntity> scheduleList;
        Elements dayElements = doc.select("li.step-item");

        if (dayElements.isEmpty()) {
            log.warn("Для группы {} расписание не найдено.", groupEntity.getGroupName());
                throw ExceptionFabric.create(ScheduleNotFoundException.class, ExceptionMessages.SCHEDULE_NOT_FOUND);
        }

        int currentYear = LocalDate.now().getYear();

        // Параллельный стрим: каждый dayElement парсится в своём потоке
        scheduleList = dayElements
                .parallelStream()
                .map(dayElement -> {
                    LocalDate lessonDate = extractDate(dayElement, currentYear);
                    if (lessonDate == null || !dayFilter.test(lessonDate)) {
                        return Collections.<ScheduleEntity>emptyList();
                    }
                    return parseLessonsForDay(dayElement, groupEntity, lessonDate, week);
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

        log.info("Найдено {} пар(ы) для группы {}", scheduleList.size(), groupEntity.getGroupName());
        return scheduleList;
    }

    /**
     * Извлекает дату (21 марта 2023) из элемента дня.
     */
    private LocalDate extractDate(Element dayElement, int currentYear) {
        String dateText = dayElement.select("span.step-title").text().trim();
        if (dateText.isEmpty()) {
            return null;
        }

        // Обычно там формат "Пн, 21 марта" -> нужно взять часть после запятой
        String[] dateParts = dateText.split(",");
        String dayPart = (dateParts.length > 1) ? dateParts[1].trim() : dateParts[0].trim();

        try {
            LocalDate parsedDate = LocalDate.parse(dayPart + " " + currentYear, DATE_FORMATTER);
            log.debug("Извлечена дата: {}", parsedDate);
            return parsedDate;
        } catch (Exception e) {
            log.error("Ошибка парсинга даты '{}': {}", dayPart, e.getMessage());
            throw ExceptionFabric.create(InvalidFormatDateException.class, ExceptionMessages.INVALID_DATE_FORMAT);
        }
    }

    /**
     * Парсит все пары для конкретного дня.
     */
    private List<ScheduleEntity> parseLessonsForDay(Element dayElement, GroupEntity groupEntity, LocalDate lessonDate, int week) {
        List<ScheduleEntity> lessons = new ArrayList<>();
        Elements lessonElements = dayElement.select("div.mb-4");

        for (Element lessonElement : lessonElements) {
            ScheduleEntity entity = parseLesson(lessonElement, groupEntity, lessonDate, week);
            if (entity != null) {
                lessons.add(entity);
            }
        }
        return lessons;
    }

    /**
     * Парсит одну пару: предмет, тип, время, преподавателя, аудиторию.
     */
    private ScheduleEntity parseLesson(Element lessonElement, GroupEntity groupEntity, LocalDate lessonDate, int week) {
        Element subjectElement = lessonElement.selectFirst("p.mb-2.fw-semi-bold.text-dark");
        if (subjectElement == null) {
            log.warn("Не найден элемент с названием предмета.");
            return null;
        }

        // Тип пары (ЛК, ПЗ, ЛР)
        Element badgeElement = subjectElement.selectFirst("span.badge");
        String badgeText = (badgeElement != null) ? badgeElement.text().trim() : "";
        LessonType lessonType = mapLessonType(badgeText);

        String fullSubjectText = subjectElement.text().trim();
        // Убираем из названия предмета сам badgeText (ЛК / ПЗ / ЛР)
        String subjectName = fullSubjectText.replace(badgeText, "").trim();

        Elements infoItems = lessonElement.select("ul.list-inline li");
        if (infoItems.isEmpty()) {
            log.warn("Не найдены элементы времени/преподавателя/аудитории для пары: {}", subjectName);
            return null;
        }

        // Первый li – время (формат "9:00 - 10:30")
        String timeRange = infoItems.get(0).text().trim().replace("–", "-");
        String[] times = timeRange.split("-");
        if (times.length < 2) {
            log.warn("Неверный формат времени: {}", timeRange);
            return null;
        }

        // Преподаватель и аудитория
        String teacherName = "";
        String classroom = "";
        if (infoItems.size() >= 3) {
            teacherName = infoItems.get(1).text().trim();
            classroom = infoItems.get(2).text().trim();
        } else if (infoItems.size() == 2) {
            // Если преподаватель отсутствует, а есть только аудитория
            classroom = infoItems.get(1).text().trim();
        }

        LocalTime startTime = LocalTime.parse(times[0].trim(), TIME_FORMATTER);
        LocalTime endTime = LocalTime.parse(times[1].trim(), TIME_FORMATTER);

        ScheduleEntity entity = ScheduleEntity.builder()
                .group(groupEntity)
                .subjectName(subjectName)
                .lessonType(lessonType)
                .lessonWeek(week)
                .teacherName(teacherName.isBlank() || teacherName.isEmpty() ? "Не указан" : teacherName)
                .classroom(classroom)
                .lessonDate(lessonDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        log.debug("Извлечена пара: {} ({}) с {} до {}", subjectName, lessonType, startTime, endTime);
        return entity;
    }

    /**
     * Преобразует строковое обозначение типа пары (например, "ЛК", "ПЗ", "ЛР") в LessonType.
     */
    private static LessonType mapLessonType(String badgeText) {
        return switch (badgeText) {
            case "ЛК" -> LessonType.LECTURE;
            case "ПЗ" -> LessonType.PRACTICAL;
            case "ЛР" -> LessonType.LAB;
            default -> LessonType.LECTURE;
        };
    }
}
