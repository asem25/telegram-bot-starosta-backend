package ru.semavin.telegrambot.services.schedules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Service;
import ru.semavin.telegrambot.dto.ScheduleDTO;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.ScheduleChangeService;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
        List<ScheduleDTO> changesDto = new ArrayList<>();
        List<ScheduleChangeEntity> actual = new ArrayList<>();
        processAddNewPairsToDay(changesDto, today, actual, changes, originalSchedule);
        if (actual.isEmpty() && changesDto.isEmpty()) {
            return originalSchedule;
        }
        originalSchedule.forEach(dto ->
                processAcceptScheduleChangeForCurrDay(actual, dto, changesDto));
        return changesDto.stream()
                .sorted(Comparator.comparing(ScheduleDTO::getStartTime))
                .toList();
    }

    public List<ScheduleDTO> mergeMultiGroups(Map<String, CompletableFuture<List<ScheduleDTO>>>
                                                      scheduleGroupChunks) {
        Map<LessonKey, ScheduleDTO> unique = new LinkedHashMap<>();
        Map<LessonKey, Set<String>> groupsByKey = new HashMap<>();

        scheduleGroupChunks.forEach((scheduleGroup, future) -> {
            processFindMultiGroups(scheduleGroup, future, unique, groupsByKey);
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
                                        CompletableFuture<List<ScheduleDTO>> future,
                                        Map<LessonKey, ScheduleDTO> unique,
                                        Map<LessonKey, Set<String>> groupsByKey) {
        List<ScheduleDTO> list = future.join();
        for (ScheduleDTO dto : list) {
            LessonKey key = new LessonKey(
                    dto.getLessonDate(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getSubjectName()
            );

            unique.putIfAbsent(key, dto);

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

    /**
     * Добавляет изменения, которые были выполнены для другого дня(перенос на другой день).
     * Оставшиеся отдает на изменения.
     */
    private void processAddNewPairsToDay(List<ScheduleDTO> changesDTOs, LocalDate today,
                                         List<ScheduleChangeEntity> actual,
                                         List<ScheduleChangeEntity> changes,
                                         List<ScheduleDTO> originalSchedule) {
        for (ScheduleChangeEntity change : changes) {
            if (change.getNewLessonDate() != null) {
                if (!change.getNewLessonDate().equals(today)) {
                    int index = -1;
                    for (ScheduleDTO scheduleDTO : originalSchedule) {
                        if (scheduleDTO.getControlSum().equals(change.getOldControlSum())) {
                            index = originalSchedule.indexOf(scheduleDTO);
                            break;
                        }
                    }
                    if (index != -1) {
                        originalSchedule.remove(index);
                    }
                    return;
                }
                ScheduleDTO newScheduleDto = ScheduleDTO.builder()
                        .subjectName(change.getSubjectName())
                        .lessonType(change.getLessonType())
                        .controlSum(change.getOldControlSum())
                        .teacherName(change.getTeacherName())
                        .classroom(change.getClassroom())
                        .groupName(change.getGroup().getGroupName())
                        .endTime(change.getNewEndTime() == null ?
                                change.getOldEndTime() : change.getNewEndTime())
                        .startTime(change.getNewStartTime() == null ?
                                change.getOldStartTime() : change.getNewStartTime())
                        .lessonDate(change.getNewLessonDate())
                        .build();
                if (change.getDescription() != null) {
                    newScheduleDto.setDescription(change.getDescription());
                }
                changesDTOs.add(newScheduleDto);
            } else {
                actual.add(change);
            }
        }
    }

    /**
     * Применяет изменения для пар в текущем дне.
     */
    private void processAcceptScheduleChangeForCurrDay(List<ScheduleChangeEntity> changes,
                                                       ScheduleDTO dto, List<ScheduleDTO> dtos) {
        for (ScheduleChangeEntity change : changes) {
            if (dto.getControlSum().equals(change.getOldControlSum())) {
                if (change.getNewLessonDate() == null) {
                    if (change.isDeleted()) {
                        return;
                    }

                    if (change.getNewStartTime() != null) {
                        dto.setStartTime(change.getNewStartTime());
                    }
                    if (change.getNewEndTime() != null) {
                        dto.setEndTime(change.getNewEndTime());
                    }
                    if (change.getDescription() != null) {
                        dto.setDescription(change.getDescription());
                    }
                    dto.setSubjectName(change.getSubjectName());
                    dto.setLessonType(change.getLessonType());
                    dto.setClassroom(change.getClassroom());

                    dtos.add(dto);
                }
                return;
            }
        }
        dtos.add(dto);
    }

    private static final class LessonKey {
        private final LocalDate lessonDate;
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final String subjectName;

        private LessonKey(LocalDate lessonDate, LocalTime startTime, LocalTime endTime, String subjectName) {
            this.lessonDate = lessonDate;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subjectName = subjectName == null ? "" : subjectName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LessonKey)) return false;
            LessonKey that = (LessonKey) o;
            if (!lessonDate.equals(that.lessonDate)) return false;
            if (!startTime.equals(that.startTime)) return false;
            if (endTime != null ? !endTime.equals(that.endTime) : that.endTime != null) return false;
            return subjectName.equals(that.subjectName);
        }

        @Override
        public int hashCode() {
            int result = lessonDate.hashCode();
            result = 31 * result + startTime.hashCode();
            result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
            result = 31 * result + subjectName.hashCode();
            return result;
        }
    }

}
