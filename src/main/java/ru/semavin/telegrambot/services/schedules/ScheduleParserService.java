package ru.semavin.telegrambot.services.schedules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.services.GroupService;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.utils.DateUtils;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.utils.exceptions.ScheduleNotFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
public class ScheduleParserService {

    // Формат для времени, как в JSON ("9:00:00")
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss");
    private static final String SCHEDULE_URL = "https://public.mai.ru/schedule/data/";

    private final SemesterService semesterService;
    private final UserService teacherService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public ScheduleParserService(SemesterService semesterService, UserService teacherService, GroupService groupService) {
        this.semesterService = semesterService;
        this.teacherService = teacherService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<ScheduleEntity> findScheduleByGroup(GroupEntity groupEntity) {
        String jsonString = getJsonOfScheduleStudentWithGroupName(groupEntity.getGroupName());
        log.info("JSON для группы {} получен", groupEntity.getGroupName());
        if (jsonString == null || jsonString.isEmpty()) {
            log.error("Получен пустой JSON для группы {}", groupEntity.getGroupName());
            throw ExceptionFabric.create(ScheduleNotFoundException.class, ExceptionMessages.SCHEDULE_NOT_FOUND);
        }
        try {
            // Парсим JSON один раз
            JsonNode rootNode = objectMapper.readTree(jsonString);
            // Кэшируем дату начала семестра
            LocalDate semesterStart = semesterService.getStartSemester();
            // Кэш для преподавателей, чтобы избежать дублирующих обращений
            Map<String, UserEntity> teacherCache = new HashMap<>();
            List<ScheduleEntity> scheduleList = extractScheduleFromJson(groupEntity, rootNode, semesterStart, teacherCache);
            log.info("Найдено {} пар для группы {}", scheduleList.size(), groupEntity.getGroupName());
            return scheduleList;
        } catch (IOException e) {
            log.error("Ошибка парсинга JSON для группы {}: {}", groupEntity.getGroupName(), e.getMessage());
            throw new RuntimeException("Ошибка парсинга JSON", e);
        }
    }

    private List<ScheduleEntity> extractScheduleFromJson(GroupEntity groupEntity, JsonNode rootNode, LocalDate semesterStart, Map<String, UserEntity> teacherCache) {
        List<ScheduleEntity> scheduleList = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = rootNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String key = entry.getKey();
            if ("group".equals(key)) {
                continue;
            }
            LocalDate dateOfKey = LocalDate.parse(key, DateUtils.FORMATTER);
            // Обрабатываем записи только после начала семестра
            if (!dateOfKey.isBefore(semesterStart)) {
                JsonNode dayNode = entry.getValue();
                JsonNode pairsNode = dayNode.get("pairs");
                if (pairsNode != null) {
                    scheduleList.addAll(parsePairs(pairsNode, groupEntity, key, teacherCache));
                }
            }
        }
        return scheduleList;
    }

    private String getJsonOfScheduleStudentWithGroupName(String groupName) {
        log.info("Получение json для группы {}", groupName);
        return restTemplate.getForObject(SCHEDULE_URL + getMd5Hash(groupName) + ".json", String.class);
    }

    private String getMd5Hash(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private List<ScheduleEntity> parsePairs(JsonNode pairsNode, GroupEntity groupEntity, String lessonDate, Map<String, UserEntity> teacherCache) {
        List<ScheduleEntity> scheduleList = new ArrayList<>();
        // Вычисляем номер недели один раз для данного дня
        int lessonWeek = Integer.parseInt(semesterService.getWeekForDate(lessonDate));
        Iterator<Map.Entry<String, JsonNode>> timeEntries = pairsNode.fields();
        while (timeEntries.hasNext()) {
            Map.Entry<String, JsonNode> timeEntry = timeEntries.next();
            JsonNode pairDetailsNode = timeEntry.getValue();
            Iterator<Map.Entry<String, JsonNode>> subjectEntries = pairDetailsNode.fields();
            while (subjectEntries.hasNext()) {
                Map.Entry<String, JsonNode> subjectEntry = subjectEntries.next();
                String subjectName = subjectEntry.getKey();
                JsonNode lessonNode = subjectEntry.getValue();

                String timeStartStr = lessonNode.path("time_start").asText();
                String timeEndStr = lessonNode.path("time_end").asText();
                LocalTime startTime = LocalTime.parse(timeStartStr, TIME_FORMATTER);
                LocalTime endTime = LocalTime.parse(timeEndStr, TIME_FORMATTER);

                String typeKey = "";
                if (lessonNode.has("type")) {
                    Iterator<String> typeFields = lessonNode.get("type").fieldNames();
                    if (typeFields.hasNext()) {
                        typeKey = typeFields.next();
                    }
                }
                LessonType lessonType = mapLessonType(typeKey);

                String classroom = "";
                if (lessonNode.has("room")) {
                    Iterator<Map.Entry<String, JsonNode>> roomFields = lessonNode.get("room").fields();
                    if (roomFields.hasNext()) {
                        classroom = roomFields.next().getValue().asText();
                    }
                }

                String teacherName = "";
                String teacherUuid = null;
                if (lessonNode.has("lector")) {
                    Iterator<Map.Entry<String, JsonNode>> lectorFields = lessonNode.get("lector").fields();
                    if (lectorFields.hasNext()) {
                        Map.Entry<String, JsonNode> lectorEntry = lectorFields.next();
                        teacherUuid = lectorEntry.getKey();
                        teacherName = lectorEntry.getValue().asText();
                    }
                }

                UserEntity teacher;
                if (teacherCache.containsKey(teacherUuid)) {
                    teacher = teacherCache.get(teacherUuid);
                } else {
                    teacher = teacherService.findOrCreateTeacherAndAddGroup(teacherUuid, teacherName, groupEntity);
                }


                ScheduleEntity entity = ScheduleEntity.builder()
                        .group(groupEntity)
                        .subjectName(subjectName)
                        .lessonType(lessonType)
                        .teacher(teacher)
                        .classroom(classroom)
                        .lessonDate(LocalDate.parse(lessonDate, DateUtils.FORMATTER))
                        .startTime(startTime)
                        .endTime(endTime)
                        .lessonWeek(lessonWeek)
                        .build();

                scheduleList.add(entity);
            }
        }
        log.debug("Найдено для даты {}: {}", lessonDate, scheduleList);
        return scheduleList;
    }

    private static LessonType mapLessonType(String badgeText) {
        return switch (badgeText) {
            case "ЛК" -> LessonType.LECTURE;
            case "ПЗ" -> LessonType.PRACTICAL;
            case "ЛР" -> LessonType.LAB;
            case "Экзамен" -> LessonType.EXAM;
            default -> LessonType.LECTURE;
        };
    }
}
