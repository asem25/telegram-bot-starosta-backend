package ru.semavin.telegrambot.services.schedules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.utils.DateUtils;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.ScheduleNotFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleParserService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm:ss");
    private static final String SCHEDULE_URL = "https://public.mai.ru/schedule/data/";

    private final SemesterService semesterService;
    private final GroupService groupService;
    private final UserService teacherService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public List<ScheduleEntity> findScheduleByGroup(GroupEntity groupEntity) {
        String jsonString = getJsonOfScheduleStudentWithGroupName(groupEntity.getGroupName());
        log.info("JSON для группы {} получен", groupEntity.getGroupName());
        if (jsonString == null || jsonString.isEmpty()) {
            log.error("Получен пустой JSON для группы {}", groupEntity.getGroupName());
            throw ExceptionFabric.create(ScheduleNotFoundException.class, ExceptionMessages.SCHEDULE_NOT_FOUND);
        }
        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            LocalDate semesterStart = semesterService.getStartSemester();
            Map<String, UserEntity> teacherCache = new ConcurrentHashMap<>();
            List<ScheduleEntity> scheduleList = extractScheduleFromJson(groupEntity, rootNode, semesterStart, teacherCache);
            log.info("Найдено {} пар для группы {}", scheduleList.size(), groupEntity.getGroupName());
            return scheduleList;
        } catch (IOException e) {
            log.error("Ошибка парсинга JSON для группы {}: {}", groupEntity.getGroupName(), e.getMessage());
            throw new RuntimeException("Ошибка парсинга JSON", e);
        }
    }

    public List<ScheduleEntity> getScheduleTeacherFromSite(String teacherUUID) {
        UserEntity teacher = teacherService.findTeacher(teacherUUID);

        String jsonString = getJsonOfScheduleTeacher(teacher);

        return parseTeacherSchedule(teacher, jsonString);
    }

    /**
     * Парсит расписание преподавателя из JSON и возвращает список ScheduleEntity.
     *
     * @param teacher    сущность преподавателя
     * @param jsonString сырой JSON, полученный с маёвского сайта
     * @return список пар преподавателя в виде ScheduleEntity
     */
    private List<ScheduleEntity> parseTeacherSchedule(UserEntity teacher, String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            log.error("Получен пустой JSON для преподавателя {}", teacher.getId());
            throw new RuntimeException("Пустой JSON расписания преподавателя");
        }

        try {
            JsonNode root = objectMapper.readTree(jsonString);

            JsonNode groupsNode = root.path("groups");
            teacher = updateTeachingGroupsFromJson(teacher, groupsNode);

            JsonNode scheduleNode = root.path("schedule");
            LocalDate semesterStart = semesterService.getStartSemester();

            return extractTeacherScheduleFromJson(teacher, scheduleNode, semesterStart);
        } catch (IOException e) {
            log.error("Ошибка парсинга JSON для преподавателя {}: {}", teacher.getId(), e.getMessage());
            throw new RuntimeException("Ошибка парсинга JSON расписания преподавателя", e);
        }
    }

    private UserEntity updateTeachingGroupsFromJson(UserEntity teacher, JsonNode groupsNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = groupsNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String groupName = entry.getKey();

            GroupEntity group = groupService.findEntityByName(groupName);

            teacher.getTeachingGroups().add(group);
        }

        return teacherService.update(teacher);
    }

    /**
     * Обходит узел "schedule" преподавателя и формирует список ScheduleEntity.
     *
     * @param teacher       преподаватель
     * @param scheduleNode  узел JSON
     * @param semesterStart дата начала семестра
     */
    private List<ScheduleEntity> extractTeacherScheduleFromJson(
            UserEntity teacher,
            JsonNode scheduleNode,
            LocalDate semesterStart
    ) {
        if (scheduleNode == null || !scheduleNode.isObject()) {
            log.warn("У преподавателя {} отсутствует корректный узел 'schedule'", teacher.getId());
            return List.of();
        }

        List<ScheduleEntity> result = streamFields(scheduleNode)
                .map(dayEntry -> {
                    String dateKey = dayEntry.getKey();
                    LocalDate lessonDate = LocalDate.parse(dateKey, DateUtils.FORMATTER);

                    if (lessonDate.isBefore(semesterStart)) {
                        return List.<ScheduleEntity>of();
                    }

                    int lessonWeek = Integer.parseInt(semesterService.getWeekForDate(dateKey));
                    JsonNode pairsNode = dayEntry.getValue().path("pairs");

                    if (pairsNode == null || !pairsNode.isObject()) {
                        return List.<ScheduleEntity>of();
                    }

                    return parseTeacherPairs(teacher, pairsNode, lessonDate, lessonWeek);
                })
                .flatMap(List::stream)
                .toList();

        log.info("Найдено {} пар для преподавателя {}", result.size(), teacher.getId());
        return result;
    }

    /**
     * Парсит блок "pairs" расписания преподавателя.
     *
     * @param teacher    преподаватель
     * @param pairsNode  узел JSON
     * @param lessonDate дата занятия
     * @param lessonWeek номер недели
     */
    private List<ScheduleEntity> parseTeacherPairs(
            UserEntity teacher,
            JsonNode pairsNode,
            LocalDate lessonDate,
            int lessonWeek
    ) {
        return streamFields(pairsNode)
                .map(Map.Entry::getValue)
                .flatMap(pairNode -> {
                    LocalTime startTime = LocalTime.parse(pairNode.path("time_start").asText(), TIME_FORMATTER);
                    LocalTime endTime = LocalTime.parse(pairNode.path("time_end").asText(), TIME_FORMATTER);

                    String subjectName = pairNode.path("name").asText();

                    LessonType lessonType = extractLessonType(pairNode.path("types"));

                    String classroom = extractFirstRoom(pairNode.path("rooms"));

                    JsonNode groupsArray = pairNode.path("groups");
                    if (!groupsArray.isArray() || groupsArray.isEmpty()) {
                        return Stream.empty();
                    }

                    List<String> groupsList =
                            streamArray(groupsArray)
                                    .map(JsonNode::asText)
                                    .filter(s -> s != null && !s.isBlank())
                                    .distinct()
                                    .sorted()
                                    .toList();

                    return groupsList.stream()
                            .map(groupService::findEntityByName)
                            .map(group -> ScheduleEntity.builder()
                                    .group(group)
                                    .teacher(teacher)
                                    .subjectName(subjectName)
                                    .lessonType(lessonType)
                                    .classroom(classroom)
                                    .lessonDate(lessonDate)
                                    .startTime(startTime)
                                    .endTime(endTime)
                                    .lessonWeek(lessonWeek)
                                    .groupsList(groupsList)
                                    .build()
                            )
                            .map(ScheduleControlSumParserService::fillCalculateSum);
                })
                .sorted(Comparator.comparing(ScheduleEntity::getLessonDate)
                        .thenComparing(ScheduleEntity::getStartTime))
                .collect(Collectors.collectingAndThen
                        (Collectors.toList(), this::mergeSchedule));
    }

    private List<ScheduleEntity> extractScheduleFromJson(GroupEntity groupEntity,
                                                         JsonNode rootNode,
                                                         LocalDate semesterStart,
                                                         Map<String, UserEntity> teacherCache) {
        List<CompletableFuture<List<ScheduleEntity>>> tasks =
                streamFields(rootNode)
                        .filter(e -> !"group".equals(e.getKey()))
                        .filter(e -> !LocalDate.parse(e.getKey(), DateUtils.FORMATTER).isBefore(semesterStart))
                        .map(e -> {
                            String date = e.getKey();
                            JsonNode pairsNode = e.getValue().path("pairs");
                            if (pairsNode.isMissingNode() || pairsNode.isNull()) {
                                return CompletableFuture.completedFuture(Collections.<ScheduleEntity>emptyList());
                            }
                            // Запускаем парсинг дня в пуле
                            return CompletableFuture.supplyAsync(
                                    () -> parsePairs(pairsNode, groupEntity, date, teacherCache),
                                    executorService
                            );
                        })
                        .toList();

        CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).join();

        return tasks.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .onClose(() -> log.info("Получено пар для группы {}", groupEntity.getGroupName()))
                .toList();
    }

    private String getJsonOfScheduleStudentWithGroupName(String groupName) {
        log.info("Получение json для группы {}", groupName);
        return restTemplate.getForObject(SCHEDULE_URL + getMd5Hash(groupName) + ".json", String.class);
    }

    private String getJsonOfScheduleTeacher(UserEntity teacher) {
        log.info("Получение json для преподавателя {}", teacher.getTeacherUuid());
        return restTemplate.getForObject(SCHEDULE_URL + teacher.getTeacherUuid() + ".json", String.class);
    }

    private String getMd5Hash(String input) {
        return DigestUtils.md5DigestAsHex(input.getBytes(StandardCharsets.UTF_8));
    }

    private Optional<String> firstFieldName(JsonNode n) {
        var it = n != null ? n.fieldNames() : null;
        return (it != null && it.hasNext()) ?
                Optional.ofNullable(it.next()) :
                Optional.empty();
    }

    private Optional<String> firstFieldValue(JsonNode n) {
        var it = n != null ? n.fields() : null;
        return (it != null && it.hasNext()) ?
                Optional.ofNullable(it.next().getValue().asText()) :
                Optional.empty();
    }

    private static Stream<Map.Entry<String, JsonNode>> streamFields(JsonNode node) {
        var it = node.fields();
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(it, 0), false);
    }


    private List<ScheduleEntity> parsePairs(JsonNode pairsNode, GroupEntity groupEntity, String lessonDate,
                                            Map<String, UserEntity> teacherCache) {
        int lessonWeek = Integer.parseInt(semesterService.getWeekForDate(lessonDate));
        return streamFields(pairsNode)
                .flatMap(timeEntry -> streamFields(timeEntry.getValue())
                        .map(subjectEntry -> Map.entry(timeEntry.getKey(), subjectEntry)))
                .map(entry -> extractScheduleEntityFromJson(groupEntity,
                        lessonDate, teacherCache,
                        entry, lessonWeek)
                )
                .sorted(Comparator.comparing(ScheduleEntity::getLessonDate)
                        .thenComparing(ScheduleEntity::getStartTime))
                .onClose(() -> log.info("Найдено расписание для группы {} дата {}",
                        groupEntity.getGroupName(),
                        lessonDate))
                .collect(Collectors.collectingAndThen
                        (Collectors.toList(), this::mergeSchedule));
    }

    private List<ScheduleEntity> mergeSchedule(List<ScheduleEntity> list) {
        List<ScheduleEntity> returning = new ArrayList<>();

        int left = 0;

        while (left < list.size()) {
            int right = left + 1;
            var tempSchedule = list.get(left);

            while (right < list.size() &&
                    isDoublePair(list.get(left), list.get(right))) {
                tempSchedule.setEndTime(list.get(right).getEndTime());
                right++;
            }
            returning.add(tempSchedule);
            left = right;
        }
        return returning;
    }

    private static boolean isDoublePair(ScheduleEntity first, ScheduleEntity second) {
        return Objects.equals(first.getSubjectName(), second.getSubjectName())
                &&
                first.getTeacher().getTeacherUuid().equalsIgnoreCase(second.getTeacher().getTeacherUuid())
                &&
                first.getLessonType().equals(second.getLessonType())
                &&
                first.getClassroom().equalsIgnoreCase(second.getClassroom())
                &&
                (Duration.between(first.getEndTime(), second.getStartTime())).toMinutes() <= 15;
    }

    private ScheduleEntity extractScheduleEntityFromJson(GroupEntity groupEntity,
                                                         String lessonDate,
                                                         Map<String, UserEntity> teacherCache,
                                                         Map.Entry<String, Map.Entry<String, JsonNode>> entry,
                                                         int lessonWeek) {
        String subjectName = entry.getValue().getKey();
        JsonNode lessonNode = entry.getValue().getValue();

        LocalTime start = LocalTime.parse(lessonNode.path("time_start").asText(), TIME_FORMATTER);
        LocalTime end = LocalTime.parse(lessonNode.path("time_end").asText(), TIME_FORMATTER);

        LessonType type = mapLessonType(firstFieldName(lessonNode.get("type")).orElse(""));
        String classroom = firstFieldValue(lessonNode.get("room")).orElse("");
        // teacher
        String teacherUuid = null, teacherName = " ";
        var lectorNode = lessonNode.get("lector");
        if (lectorNode != null && lectorNode.fieldNames().hasNext()) {
            var e = lectorNode.fields().next();
            teacherUuid = e.getKey();
            teacherName = e.getValue().asText();
        }
        UserEntity teacher;
        if (teacherCache.containsKey(teacherUuid)) {
            teacher = teacherCache.get(teacherUuid);
        } else {
            teacher = teacherService.findOrCreateTeacherAndAddGroup(teacherUuid, teacherName, groupEntity);
        }
        log.debug("Найдено расписание\nдля даты {}\nдля группы{}\nпрепод{}\nпредмет{}",
                lessonDate,
                groupEntity.getGroupName(),
                teacher.getTeacherUuid(),
                subjectName);
        return ScheduleControlSumParserService.fillCalculateSum(
                buildScheduleEntity(groupEntity,
                        lessonDate,
                        lessonWeek,
                        subjectName,
                        type,
                        classroom,
                        teacher,
                        start, end));
    }

    private static ScheduleEntity buildScheduleEntity(GroupEntity groupEntity, String lessonDate,
                                                      int lessonWeek, String subjectName,
                                                      LessonType type, String classroom, UserEntity teacher,
                                                      LocalTime start, LocalTime end) {
        return ScheduleEntity.builder()
                .group(groupEntity)
                .lessonDate(LocalDate.parse(lessonDate, DateUtils.FORMATTER))
                .lessonWeek(lessonWeek)
                .subjectName(subjectName)
                .lessonType(type)
                .classroom(classroom)
                .teacher(teacher)
                .startTime(start)
                .endTime(end)
                .build();
    }

    private java.util.stream.Stream<JsonNode> streamArray(JsonNode arrayNode) {
        Iterable<JsonNode> iterable = arrayNode::elements;
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }

    private LessonType extractLessonType(JsonNode typesNode) {
        if (typesNode != null && typesNode.isArray() && !typesNode.isEmpty()) {
            return mapLessonType(typesNode.get(0).asText());
        }
        return mapLessonType("");
    }

    private String extractFirstRoom(JsonNode roomsNode) {
        if (roomsNode != null && roomsNode.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = roomsNode.fields();
            if (it.hasNext()) {
                return it.next().getValue().asText();
            }
        }
        return "";
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
