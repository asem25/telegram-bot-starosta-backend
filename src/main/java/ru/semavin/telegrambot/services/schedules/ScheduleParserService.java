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


    private List<ScheduleEntity> parsePairs(JsonNode pairsNode, GroupEntity groupEntity, String lessonDate, Map<String, UserEntity> teacherCache) {
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

    private static ScheduleEntity buildScheduleEntity(GroupEntity groupEntity, String lessonDate, int lessonWeek, String subjectName, LessonType type, String classroom, UserEntity teacher, LocalTime start, LocalTime end) {
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
