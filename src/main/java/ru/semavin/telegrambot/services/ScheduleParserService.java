package ru.semavin.telegrambot.services;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.enums.LessonType;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис для парсинга расписания занятий с сайта МАИ.
 * <p>
 * Процесс парсинга разбит на несколько шагов:
 * <ol>
 *   <li>Получение куки и загрузка HTML-страницы расписания для заданной группы.</li>
 *   <li>Парсинг элементов дня (li.step-item) и извлечение даты.</li>
 *   <li>Парсинг пар для каждого дня и формирование объектов {@link ScheduleEntity}.</li>
 * </ol>
 */
@Slf4j
@Service
public class ScheduleParserService {
    private static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
    private final int CURRENT_WEEK;
    @Autowired
    public ScheduleParserService(SemesterService semesterService) {
        this.CURRENT_WEEK = Integer.parseInt(semesterService.getCurrentWeek());
    }

    /**
     * Преобразует строковое обозначение типа пары (например, "ЛК", "ПЗ", "ЛР") в LessonType.
     *
     * @param badgeText Текст из бейджа
     * @return Соответствующий LessonType; если тип не распознан – возвращается LECTURE
     */
    private static LessonType mapLessonType(String badgeText) {
        badgeText = badgeText.trim();
        return switch (badgeText) {
            case "ЛК" -> LessonType.LECTURE;
            case "ПЗ" -> LessonType.PRACTICAL;
            case "ЛР" -> LessonType.LAB;
            default -> LessonType.LECTURE;
        };
    }

    /**
     * Основной метод сервиса, который для заданной группы получает куки, загружает HTML-страницу расписания
     * и парсит её в список объектов {@link ScheduleEntity}.
     *
     * @param groupName Имя группы (например, "М3О-303С-22")
     * @param week Номер учебной недели текущего семестра(если null, то текущая неделя)
     * @return Список объектов расписания; если произошла ошибка, возвращается пустой список
     * @throws IOException Если возникает ошибка подключения
     */
    public List<ScheduleEntity> findScheduleByGroup(String groupName, String week) throws IOException {
        log.info("Начало парсинга расписания для группы, текущей недели: {}", groupName);
        try {
            Map<String, String> cookies = getCookiesForGroup(groupName);
            log.info("Получены куки для группы {}: {}", groupName, cookies);
            Document schedulePage = getSchedulePage(groupName, cookies, week);
            log.info("Загружена страница расписания для группы: {}", groupName);
            List<ScheduleEntity> schedule = parseScheduleForWeek(schedulePage, groupName);
            log.info("Найдено {} пар для группы {}", schedule.size(), groupName);
            return schedule;
        } catch (IOException e) {
            log.error("Ошибка при парсинге расписания для группы {}: {}", groupName, e.getMessage(), e);
        }
        return new ArrayList<>();
    }
    public List<ScheduleEntity> findScheduleByGroup(String groupName) throws IOException {
        log.info("Начало парсинга расписания для группы, текущего дня: {}", groupName);
        try {
            String week = null;
            Map<String, String> cookies = getCookiesForGroup(groupName);
            log.info("Получены куки для группы {}: {}", groupName, cookies);
            Document schedulePage = getSchedulePage(groupName, cookies, week);
            log.info("Загружена страница расписания для группы: {}", groupName);
            List<ScheduleEntity> schedule = parseScheduleForCurrentDay(schedulePage, groupName);
            log.info("Найдено {} пар для группы {}", schedule.size(), groupName);
            return schedule;
        } catch (IOException e) {
            log.error("Ошибка при парсинге расписания для группы {}: {}", groupName, e.getMessage(), e);
        }
        return new ArrayList<>();
    }

    /**
     * Выполняет запрос к странице расписания для заданной группы и возвращает полученные куки.
     *
     * @param groupName Имя группы
     * @return Map с куками (ключ – имя куки, значение – её значение)
     * @throws IOException Если происходит ошибка при выполнении запроса
     */
    private static Map<String, String> getCookiesForGroup(String groupName) throws IOException {
        String url = "https://mai.ru/education/studies/schedule/index.php?group=" +
                URLEncoder.encode(groupName, StandardCharsets.UTF_8);
        log.debug("Получаем куки с URL: {}", url);
        Connection.Response response = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://mai.ru/education/studies/schedule/")
                .execute();
        return response.cookies();
    }

    /**
     * Загружает HTML-страницу расписания для указанной группы, с учетом выбранной учебной недели,
     * используя переданные куки.
     *
     * @param groupName Имя группы
     * @param cookies   Куки, полученные ранее
     * @param week      Номер учебной недели тек.семестра (например, "1", "2" и т.д.). Если null или пустая строка, параметр не добавляется.
     * @return Document с HTML-страницей расписания
     * @throws IOException Если происходит ошибка при выполнении запроса
     */
    private Document getSchedulePage(String groupName, Map<String, String> cookies, String week) throws IOException {
        String url = "https://mai.ru/education/studies/schedule/index.php?group=" +
                URLEncoder.encode(groupName, StandardCharsets.UTF_8);
        if (week == null) {;
            url += "&week=" + URLEncoder.encode(String.valueOf(CURRENT_WEEK), StandardCharsets.UTF_8);
        }else {
            url += "&week=" + URLEncoder.encode(week, StandardCharsets.UTF_8);
        }
        log.debug("Запрос страницы расписания с URL: {}", url);
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .referrer("https://mai.ru/education/studies/schedule/")
                .cookies(cookies)
                .get();
    }

    /**
     * Парсит HTML-страницу расписания и возвращает список объектов {@link ScheduleEntity}.
     *
     * @param doc       HTML-документ с расписанием
     * @param groupName Имя группы
     * @return Список объектов расписания
     */
    private List<ScheduleEntity> parseScheduleForWeek(Document doc, String groupName) {
        List<ScheduleEntity> scheduleList = new ArrayList<>();
        Elements dayElements = doc.select("li.step-item");
        if (dayElements.isEmpty()) {
            log.warn("Для группы {} расписание не найдено.", groupName);
            return scheduleList;
        }

        int currentYear = LocalDate.now().getYear();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        for (Element dayElement : dayElements) {
            LocalDate lessonDate = extractDate(dayElement, currentYear, dateFormatter);
            if (lessonDate == null) {
                log.warn("Пропускаем день из-за ошибки парсинга даты.");
                continue;
            }
            List<ScheduleEntity> dayLessons = parseLessonsForDay(dayElement, groupName, lessonDate, timeFormatter);
            scheduleList.addAll(dayLessons);
        }
        return scheduleList;
    }

    private List<ScheduleEntity> parseScheduleForCurrentDay(Document doc, String groupName){
        List<ScheduleEntity> scheduleList = new ArrayList<>();
        Elements dayElements = doc.select("li.step-item");
        if (dayElements.isEmpty()) {
            log.warn("Для группы {} расписание не найдено.", groupName);
            return scheduleList;
        }

        int currentYear = LocalDate.now().getYear();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru"));
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm");

        for (Element dayElement : dayElements) {
            LocalDate lessonDate = extractDate(dayElement, currentYear, dateFormatter);
            if (lessonDate != null) {
                if (lessonDate.getDayOfWeek() != LocalDate.now().getDayOfWeek()) {
                    continue;
                }
            }
            List<ScheduleEntity> dayLessons = parseLessonsForDay(dayElement, groupName, lessonDate, timeFormatter);
            scheduleList.addAll(dayLessons);
            break;
        }
        return scheduleList;
    }
    /**
     * Извлекает дату пары из элемента дня.
     *
     * @param dayElement    Элемент, содержащий данные дня (например, "Пн, 21 марта")
     * @param currentYear   Текущий год (для формирования полного формата даты)
     * @param dateFormatter Форматтер для парсинга даты
     * @return Объект LocalDate, если удалось распарсить дату, иначе null
     */
    private static LocalDate extractDate(Element dayElement, int currentYear, DateTimeFormatter dateFormatter) {
        String dateText = dayElement.select("span.step-title").text().trim();
        String[] dateParts = dateText.split(",");
        String dayPart = dateParts.length > 1 ? dateParts[1].trim() : dateParts[0].trim();
        try {
            LocalDate parsedDate = LocalDate.parse(dayPart + " " + currentYear, dateFormatter);
            log.debug("Извлечена дата: {} для текста: {}", parsedDate, dayPart);
            return parsedDate;
        } catch (Exception e) {
            log.error("Ошибка парсинга даты '{}': {}", dayPart, e.getMessage());
            return null;
        }
    }

    /**
     * Парсит все пары для одного дня.
     *
     * @param dayElement    Элемент дня расписания
     * @param groupName     Имя группы
     * @param lessonDate    Дата занятий (полученная из элемента дня)
     * @param timeFormatter Форматтер для парсинга времени
     * @return Список объектов ScheduleEntity, соответствующих пар данного дня
     */
    private  List<ScheduleEntity> parseLessonsForDay(Element dayElement, String groupName, LocalDate lessonDate, DateTimeFormatter timeFormatter) {
        List<ScheduleEntity> lessons = new ArrayList<>();
        Elements lessonElements = dayElement.select("div.mb-4");
        log.debug("Найдено {} пар для дня {} группы {}", lessonElements.size(), lessonDate, groupName);
        for (Element lessonElement : lessonElements) {
            ScheduleEntity entity = parseLesson(lessonElement, groupName, lessonDate, timeFormatter);
            if (entity != null) {
                lessons.add(entity);
            }
        }
        return lessons;
    }

    /**
     * Парсит данные одной пары из элемента lessonElement и возвращает объект {@link ScheduleEntity}.
     *
     * @param lessonElement Элемент, содержащий данные пары
     * @param groupName     Имя группы
     * @param lessonDate    Дата пары
     * @param timeFormatter Форматтер для парсинга времени (например, "H:mm")
     * @return Объект ScheduleEntity с данными пары или null, если извлечение не удалось
     */
    private ScheduleEntity parseLesson(Element lessonElement, String groupName, LocalDate lessonDate, DateTimeFormatter timeFormatter) {
        Element subjectElement = lessonElement.selectFirst("p.mb-2.fw-semi-bold.text-dark");
        if (subjectElement == null) {
            log.warn("Не найден элемент с названием предмета.");
            return null;
        }

        String badgeText = "";
        Element badgeElement = subjectElement.selectFirst("span.badge");
        if (badgeElement != null) {
            badgeText = badgeElement.text().trim();
        }

        String fullSubjectText = subjectElement.text().trim();
        String subjectName = fullSubjectText.replace(badgeText, "").trim();
        LessonType lessonType = mapLessonType(badgeText);

        Elements infoItems = lessonElement.select("ul.list-inline li");
        if (infoItems.isEmpty()) {
            log.warn("Не найдены информационные элементы (время, преподаватель, аудитория) для пары: {}", subjectName);
            return null;
        }
        String timeRange = infoItems.get(0).text().trim().replace("–", "-");
        String teacherName = "";
        String classroom = "";
        if (infoItems.size() >= 3) {
            teacherName = infoItems.get(1).text().trim();
            classroom = infoItems.get(2).text().trim();
        } else if (infoItems.size() == 2) {
            classroom = infoItems.get(1).text().trim();
        } else {
            log.warn("Недостаточно информационных элементов для пары: {}", subjectName);
            return null;
        }

        String[] times = timeRange.split("-");
        if (times.length < 2) {
            log.warn("Неверный формат времени: {}", timeRange);
            return null;
        }
        LocalTime startTime = LocalTime.parse(times[0].trim(), timeFormatter);
        LocalTime endTime = LocalTime.parse(times[1].trim(), timeFormatter);

        ScheduleEntity entity = ScheduleEntity.builder()
                .groupName(groupName)
                .subjectName(subjectName)
                .lessonType(lessonType)
                .lessonWeek(CURRENT_WEEK)
                .teacherName(teacherName)
                .classroom(classroom)
                .lessonDate(lessonDate)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        log.debug("Извлечена пара: {} ({}) с {} до {}", subjectName, lessonType, startTime, endTime);
        return entity;
    }


}
