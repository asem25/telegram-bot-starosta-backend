package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleMergingService {
    private final ScheduleRepository scheduleRepository;
    private final ScheduleMapper scheduleMapper;
    private final SemesterService semesterService;
    private final GroupService groupService;
    private final ScheduleChangeService scheduleChangeService;

    /**
     * Получаем все расписание для семестра с учетом слияния
     */
    public List<ScheduleDTO> getScheduleAfterMerge(String groupName) {
        val group = groupService.findEntityByName(groupName);
        List<ScheduleDTO> scheduleDTOS = new ArrayList<>();
        for (LocalDate date = semesterService.getStartSemester();
             !date.isAfter(semesterService.getEndSemester());
             date = date.plusDays(1)) {

            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }

            val changes = scheduleChangeService.getChangesDtoAnyDay(groupName, date);
            val scheduleForMerge = scheduleRepository.findAllByLessonDateAndGroup(date, group);
            val original = scheduleMapper.toScheduleDTOList(scheduleForMerge);

            scheduleDTOS.addAll(mergeChanges(original, changes, date));
        }
        return scheduleDTOS;
    }

    /**
     * Сливает расписания. Возможны два случая:
     * 1. Перенесли пару на другой день (processAddNewPairsToDay)
     * 2. Не переносили, просто изменения (processAcceptScheduleChangeForCurrDay)
     */
    public List<ScheduleDTO> mergeChanges(
            List<ScheduleDTO> originalSchedule,
            List<ScheduleChangeEntity> changes,
            LocalDate today
    ) {
        Objects.requireNonNull(today, "today must not be null");

        Map<String, ScheduleDTO> lessonsByControlSum = indexLessonsByControlSum(originalSchedule);
        applyChanges(changes, today, lessonsByControlSum);
        return combineAndSort(lessonsWithoutControlSum(originalSchedule), lessonsByControlSum);
    }

    private Map<String, ScheduleDTO> indexLessonsByControlSum(List<ScheduleDTO> schedule) {
        return schedule.stream()
                .map(this::copyOf)
                .filter(dto -> dto.getLessonOccurrenceId() != null || dto.getControlSum() != null)
                .collect(Collectors.toMap(
                        this::lessonKey,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
    }

    private List<ScheduleDTO> lessonsWithoutControlSum(List<ScheduleDTO> schedule) {
        return schedule.stream()
                .filter(dto -> dto.getLessonOccurrenceId() == null && dto.getControlSum() == null)
                .map(this::copyOf)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void applyChanges(List<ScheduleChangeEntity> changes, LocalDate date,
                              Map<String, ScheduleDTO> lessonsByControlSum) {
        changes.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        ScheduleChangeEntity::getId,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(change -> applyChange(change, date, lessonsByControlSum));
    }

    private List<ScheduleDTO> combineAndSort(List<ScheduleDTO> lessonsWithoutControlSum,
                                             Map<String, ScheduleDTO> lessonsByControlSum) {
        lessonsWithoutControlSum.addAll(lessonsByControlSum.values());
        lessonsWithoutControlSum.sort(Comparator
                .comparing(ScheduleDTO::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScheduleDTO::getEndTime, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ScheduleDTO::getSubjectName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return List.copyOf(lessonsWithoutControlSum);
    }

    private void applyChange(ScheduleChangeEntity change, LocalDate today,
                             Map<String, ScheduleDTO> lessonsByControlSum) {
        boolean originatesToday = today.equals(change.getOldLessonDate());
        boolean targetsToday = today.equals(change.getNewLessonDate());
        String controlSum = change.getOccurrenceId() == null
                ? change.getOldControlSum()
                : change.getOccurrenceId().toString();

        if (originatesToday && (change.isDeleted() ||
                (change.getNewLessonDate() != null && !targetsToday))) {
            lessonsByControlSum.remove(controlSum);
        }

        if (change.isDeleted()) {
            return;
        }

        if (targetsToday) {
            ScheduleDTO original = lessonsByControlSum.get(controlSum);
            lessonsByControlSum.put(controlSum, changedLesson(change, original, today));
            return;
        }

        if (originatesToday && change.getNewLessonDate() == null) {
            ScheduleDTO original = lessonsByControlSum.get(controlSum);
            if (original != null) {
                lessonsByControlSum.put(controlSum, changedLesson(change, original, today));
            }
        }
    }

    private String lessonKey(ScheduleDTO lesson) {
        return lesson.getLessonOccurrenceId() == null
                ? lesson.getControlSum()
                : lesson.getLessonOccurrenceId().toString();
    }

    private ScheduleDTO changedLesson(ScheduleChangeEntity change, ScheduleDTO original, LocalDate date) {
        return ScheduleDTO.builder()
                .id(originalValue(original, ScheduleDTO::getId))
                .lessonOccurrenceId(change.getOccurrenceId() != null
                        ? change.getOccurrenceId()
                        : originalValue(original, ScheduleDTO::getLessonOccurrenceId))
                .lessonSeriesId(change.getSeriesId() != null
                        ? change.getSeriesId()
                        : originalValue(original, ScheduleDTO::getLessonSeriesId))
                .version(change.getVersion() == null ? 0 : change.getVersion())
                .groupName(changedGroupName(change, original))
                .subjectName(changedText(change.getSubjectName(), original, ScheduleDTO::getSubjectName))
                .lessonType(changedText(change.getLessonType(), original, ScheduleDTO::getLessonType))
                .teacherName(changedText(change.getTeacherName(), original, ScheduleDTO::getTeacherName))
                .classroom(changedText(change.getClassroom(), original, ScheduleDTO::getClassroom))
                .description(changedValue(change.getDescription(), original, ScheduleDTO::getDescription, null))
                .lessonDate(date)
                .startTime(changedValue(change.getNewStartTime(), original,
                        ScheduleDTO::getStartTime, change.getOldStartTime()))
                .endTime(changedValue(change.getNewEndTime(), original,
                        ScheduleDTO::getEndTime, change.getOldEndTime()))
                .controlSum(change.getOldControlSum())
                .build();
    }

    private String changedGroupName(ScheduleChangeEntity change, ScheduleDTO original) {
        String changedGroup = Optional.ofNullable(change.getGroup())
                .map(GroupEntity::getGroupName)
                .orElse(null);
        return firstNonBlank(changedGroup, originalValue(original, ScheduleDTO::getGroupName));
    }

    private String changedText(String changed, ScheduleDTO original,
                               Function<ScheduleDTO, String> extractor) {
        return firstNonBlank(changed, originalValue(original, extractor));
    }

    private <T> T changedValue(T changed, ScheduleDTO original,
                               Function<ScheduleDTO, T> extractor, T fallback) {
        if (changed != null) {
            return changed;
        }
        return Optional.ofNullable(original)
                .map(extractor)
                .orElse(fallback);
    }

    private <T> T originalValue(ScheduleDTO original, Function<ScheduleDTO, T> value) {
        return Optional.ofNullable(original)
                .map(value)
                .orElse(null);
    }

    private String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred;
    }

    private ScheduleDTO copyOf(ScheduleDTO source) {
        return ScheduleDTO.builder()
                .id(source.getId())
                .lessonOccurrenceId(source.getLessonOccurrenceId())
                .lessonSeriesId(source.getLessonSeriesId())
                .version(source.getVersion())
                .groupName(source.getGroupName())
                .subjectName(source.getSubjectName())
                .lessonType(source.getLessonType())
                .teacherName(source.getTeacherName())
                .classroom(source.getClassroom())
                .description(source.getDescription())
                .lessonDate(source.getLessonDate())
                .startTime(source.getStartTime())
                .endTime(source.getEndTime())
                .controlSum(source.getControlSum())
                .build();
    }

    public List<ScheduleDTO> mergeMultiGroups(Map<String, List<ScheduleDTO>>
                                                      scheduleGroupChunks) {
        Map<LessonKey, ScheduleDTO> unique = new LinkedHashMap<>();
        Map<LessonKey, Set<String>> groupsByKey = new HashMap<>();

        scheduleGroupChunks.forEach((scheduleGroup, scheduleDTOS) -> {
            processFindMultiGroups(scheduleGroup, scheduleDTOS, unique, groupsByKey);
        });

        unique.forEach((key, dto) -> {
            setMultiGroups(key, dto, groupsByKey);
        });

        return unique.values().stream()
                .sorted(Comparator
                        .comparing(ScheduleDTO::getLessonDate)
                        .thenComparing(ScheduleDTO::getStartTime))
                .toList();
    }

    private void setMultiGroups(LessonKey key, ScheduleDTO dto,
                                Map<LessonKey, Set<String>> groupsByKey) {
        Set<String> groups = groupsByKey.get(key);
        if (groups != null && !groups.isEmpty()) {
            String merged = groups.stream().sorted().collect(Collectors.joining(", "));
            dto.setGroupName(merged);
        }
    }

    private void processFindMultiGroups(String scheduleGroup,
                                        List<ScheduleDTO> list,
                                        Map<LessonKey, ScheduleDTO> unique,
                                        Map<LessonKey, Set<String>> groupsByKey) {
        for (ScheduleDTO dto : list) {
            LessonKey key = new LessonKey(
                    dto.getLessonDate(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getSubjectName(),
                    dto.getLessonType(),
                    dto.getTeacherName(),
                    dto.getClassroom(),
                    dto.getDescription()
            );

            unique.putIfAbsent(key, copyOf(dto));

            Set<String> groups = groupsByKey.computeIfAbsent(key, k -> new HashSet<>());
            if (scheduleGroup != null && !scheduleGroup.isBlank()) {
                groups.add(scheduleGroup.trim());
            }
            if (dto.getGroupName() != null && !dto.getGroupName().isBlank()) {
                for (String g : dto.getGroupName().split(",")) {
                    String trimmed = g.trim();
                    if (!trimmed.isEmpty()) {
                        groups.add(trimmed);
                    }
                }
            }
        }
    }

    private static final class LessonKey {
        private final LocalDate lessonDate;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final String subjectName;
        private final String lessonType;
        private final String teacherName;
        private final String classroom;
        private final String description;

        private LessonKey(LocalDate lessonDate, LocalTime startTime, LocalTime endTime, String subjectName,
                          String lessonType, String teacherName, String classroom, String description) {
            this.lessonDate = lessonDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subjectName = normalize(subjectName);
            this.lessonType = normalize(lessonType);
            this.teacherName = normalize(teacherName);
            this.classroom = normalize(classroom);
            this.description = normalize(description);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LessonKey)) return false;
            LessonKey that = (LessonKey) o;
            if (!lessonDate.equals(that.lessonDate)) return false;
            if (!startTime.equals(that.startTime)) return false;
            return Objects.equals(endTime, that.endTime)
                    && subjectName.equals(that.subjectName)
                    && lessonType.equals(that.lessonType)
                    && teacherName.equals(that.teacherName)
                    && classroom.equals(that.classroom)
                    && description.equals(that.description);
        }

        @Override
        public int hashCode() {
            int result = lessonDate.hashCode();
            result = 31 * result + startTime.hashCode();
            result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
            result = 31 * result + subjectName.hashCode();
            result = 31 * result + lessonType.hashCode();
            result = 31 * result + teacherName.hashCode();
            result = 31 * result + classroom.hashCode();
            result = 31 * result + description.hashCode();
            return result;
        }

        private static String normalize(String value) {
            return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        }
    }

}
