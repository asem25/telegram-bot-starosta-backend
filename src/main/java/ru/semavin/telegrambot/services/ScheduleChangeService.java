package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import ru.semavin.telegrambot.dto.ScheduleChangeDTO;
import ru.semavin.telegrambot.dto.MiniAppScheduleChangeRequest;
import ru.semavin.telegrambot.dto.MiniAppScheduleChangeResponse;
import ru.semavin.telegrambot.dto.ScheduleChangeForEveryDayCheckDTO;
import ru.semavin.telegrambot.dto.ScheduleChangeForFrontDTO;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.NotificationHistoryEntity;
import ru.semavin.telegrambot.models.ScheduleChangeEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.repositories.ScheduleChangeRepository;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.NotificationHistoryRepository;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ScheduleChangeOperation;
import ru.semavin.telegrambot.models.enums.ScheduleChangeScope;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.services.schedules.ScheduleSeriesIdService;
import ru.semavin.telegrambot.utils.DateUtils;
import ru.semavin.telegrambot.utils.exceptions.ScheduleChangeConflictException;
import ru.semavin.telegrambot.utils.exceptions.ScheduleChangeForbiddenException;
import ru.semavin.telegrambot.utils.exceptions.ScheduleNotFoundException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.HexFormat;
import java.util.Objects;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Comparator;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class ScheduleChangeService {
    private final ScheduleChangeRepository changeRepository;
    private final ScheduleRepository scheduleRepository;
    private final GroupService groupService;
    private final UserService userService;
    private final GroupRepository groupRepository;
    private final CacheManager cacheManager;
    private final NotificationHistoryRepository notificationHistoryRepository;

    @Transactional
    public MiniAppScheduleChangeResponse apply(
            MiniAppScheduleChangeRequest request,
            long telegramId
    ) {
        UserEntity author = userService.findByTelegramId(telegramId);
        GroupEntity group = requireEditableGroup(author);
        String requestHash = requestHash(request);
        MiniAppScheduleChangeResponse replay = replayIfPresent(request, group, requestHash);
        if (replay != null) {
            return replay;
        }

        ScheduleEntity anchor = requireGroupOccurrence(request, group);
        ScheduleChangeEntity anchorCurrent = latestChange(anchor.getOccurrenceId());
        requireExpectedVersion(request, anchorCurrent);
        validateMove(request);

        List<ScheduleEntity> targets = resolveTargets(request, group, anchor);
        BatchSave batch = saveSnapshots(
                request, targets, anchor, anchorCurrent, group, author, requestHash);
        recordNotifications(batch.saved(), group);
        evictAffectedDays(group, batch);
        return batch.saved().stream()
                .filter(change -> change.getOccurrenceId().equals(anchor.getOccurrenceId()))
                .findFirst()
                .map(MiniAppScheduleChangeResponse::from)
                .orElseThrow(() -> new IllegalStateException("Изменение выбранной пары не сохранено"));
    }

    private void recordNotifications(List<ScheduleChangeEntity> changes, GroupEntity group) {
        for (ScheduleChangeEntity change : changes) {
            for (UserEntity recipient : group.getUsers()) {
                if (!recipient.isScheduleNotificationsEnabled()) {
                    continue;
                }
                UUID eventId = notificationEventId(change, recipient);
                if (notificationHistoryRepository.existsByEventId(eventId)) {
                    continue;
                }
                notificationHistoryRepository.save(NotificationHistoryEntity.builder()
                        .eventId(eventId)
                        .user(recipient)
                        .type("SCHEDULE_CHANGE")
                        .title(change.isDeleted() ? "Пара отменена" : "Расписание изменено")
                        .body(notificationBody(change))
                        .createdAt(OffsetDateTime.now())
                        .telegramDeliveryStatus(recipient.isTelegramWriteAccessGranted()
                                ? "PENDING"
                                : "DISABLED")
                        .build());
            }
        }
    }

    private UUID notificationEventId(ScheduleChangeEntity change, UserEntity recipient) {
        return UUID.nameUUIDFromBytes((change.getOccurrenceId() + ":" + change.getVersion()
                + ":" + recipient.getId()).getBytes(StandardCharsets.UTF_8));
    }

    private String notificationBody(ScheduleChangeEntity change) {
        String date = change.getNewLessonDate() == null ? "" : " — " + change.getNewLessonDate();
        String description = change.getDescription() == null || change.getDescription().isBlank()
                ? ""
                : ". " + change.getDescription();
        return change.getSubjectName() + date + description;
    }

    private GroupEntity requireEditableGroup(UserEntity author) {
        if (author.getGroup() == null) {
            throw forbidden();
        }
        GroupEntity group = groupRepository.findByIdForUpdate(author.getGroup().getId())
                .orElseThrow(this::forbidden);
        requireActualStarosta(author, group);
        return group;
    }

    private MiniAppScheduleChangeResponse replayIfPresent(
            MiniAppScheduleChangeRequest request,
            GroupEntity group,
            String requestHash
    ) {
        List<ScheduleChangeEntity> batch = changeRepository
                .findAllByGroupAndBatchRequestIdOrderByIdAsc(group, request.clientRequestId());
        if (!batch.isEmpty()) {
            requireSameRequestHash(batch.getFirst(), requestHash);
            return batch.stream()
                    .filter(change -> request.lessonOccurrenceId().equals(change.getOccurrenceId()))
                    .findFirst()
                    .map(MiniAppScheduleChangeResponse::from)
                    .orElseGet(() -> MiniAppScheduleChangeResponse.from(batch.getFirst()));
        }
        var legacy = changeRepository.findByGroupAndClientRequestId(group, request.clientRequestId());
        if (legacy.isEmpty()) {
            return null;
        }
        requireSameRequestHash(legacy.get(), requestHash);
        return MiniAppScheduleChangeResponse.from(legacy.get());
    }

    private void requireSameRequestHash(ScheduleChangeEntity prior, String requestHash) {
        if (!Objects.equals(prior.getRequestHash(), requestHash)) {
            throw new ScheduleChangeConflictException(
                    "clientRequestId уже использован для другого изменения");
        }
    }

    private ScheduleEntity requireGroupOccurrence(
            MiniAppScheduleChangeRequest request,
            GroupEntity group
    ) {
        ScheduleEntity source = scheduleRepository.findByOccurrenceIdForUpdate(request.lessonOccurrenceId())
                .orElseThrow(() -> new ScheduleNotFoundException("Пара не найдена"));
        if (!source.getGroup().getId().equals(group.getId())) {
            throw forbidden();
        }
        return source;
    }

    private ScheduleChangeEntity latestChange(UUID occurrenceId) {
        return changeRepository.findFirstByOccurrenceIdOrderByVersionDesc(occurrenceId)
                .orElse(null);
    }

    private long requireExpectedVersion(
            MiniAppScheduleChangeRequest request,
            ScheduleChangeEntity current
    ) {
        long currentVersion = current == null || current.getVersion() == null ? 0 : current.getVersion();
        if (request.expectedVersion() != currentVersion) {
            throw new ScheduleChangeConflictException(
                    "Расписание уже изменено. Обновите данные и повторите запрос");
        }
        return currentVersion;
    }

    private List<ScheduleEntity> resolveTargets(
            MiniAppScheduleChangeRequest request,
            GroupEntity group,
            ScheduleEntity anchor
    ) {
        if (request.scope() == ScheduleChangeScope.SINGLE) {
            return List.of(anchor);
        }
        UUID seriesId = ScheduleSeriesIdService.resolve(anchor);
        return scheduleRepository.findAllByGroup(group).stream()
                .filter(lesson -> !lesson.getLessonDate().isBefore(anchor.getLessonDate()))
                .filter(lesson -> ScheduleSeriesIdService.resolve(lesson).equals(seriesId))
                .sorted(Comparator.comparing(ScheduleEntity::getLessonDate)
                        .thenComparing(ScheduleEntity::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private BatchSave saveSnapshots(
            MiniAppScheduleChangeRequest request,
            List<ScheduleEntity> targets,
            ScheduleEntity anchor,
            ScheduleChangeEntity anchorCurrent,
            GroupEntity group,
            UserEntity author,
            String requestHash
    ) {
        LocalDate anchorVisibleDate = visibleDate(anchor, anchorCurrent);
        long shiftDays = request.operation() == ScheduleChangeOperation.MOVE
                ? ChronoUnit.DAYS.between(anchorVisibleDate, request.newLessonDate())
                : 0;
        List<PendingSnapshot> pending = targets.stream()
                .map(source -> {
                    ScheduleChangeEntity current = source.getOccurrenceId().equals(anchor.getOccurrenceId())
                            ? anchorCurrent
                            : latestChange(source.getOccurrenceId());
                    long version = current == null || current.getVersion() == null
                            ? 1
                            : current.getVersion() + 1;
                    LocalDate targetDate = request.operation() == ScheduleChangeOperation.MOVE
                            ? visibleDate(source, current).plusDays(shiftDays)
                            : visibleDate(source, current);
                    ScheduleChangeEntity snapshot = buildSnapshot(
                            request, source, current, group, author, requestHash, version,
                            targetDate, childRequestId(request, source));
                    return new PendingSnapshot(visibleDate(source, current), snapshot);
                })
                .toList();
        List<ScheduleChangeEntity> saved = changeRepository.saveAllAndFlush(pending.stream()
                .map(PendingSnapshot::snapshot)
                .toList());
        return new BatchSave(pending.stream().map(PendingSnapshot::previousDate).toList(), saved);
    }

    private UUID childRequestId(MiniAppScheduleChangeRequest request, ScheduleEntity source) {
        if (request.scope() == ScheduleChangeScope.SINGLE) {
            return request.clientRequestId();
        }
        return UUID.nameUUIDFromBytes((request.clientRequestId() + "|" + source.getOccurrenceId())
                .getBytes(StandardCharsets.UTF_8));
    }

    private void evictAffectedDays(
            GroupEntity group,
            BatchSave batch
    ) {
        batch.previousDates().forEach(date -> evictDay(group.getGroupName(), date));
        batch.saved().forEach(change -> evictDay(group.getGroupName(), change.getNewLessonDate()));
    }

    private LocalDate visibleDate(ScheduleEntity source, ScheduleChangeEntity current) {
        return current == null || current.getNewLessonDate() == null
                ? source.getLessonDate()
                : current.getNewLessonDate();
    }

    private record PendingSnapshot(LocalDate previousDate, ScheduleChangeEntity snapshot) {
    }

    private record BatchSave(List<LocalDate> previousDates, List<ScheduleChangeEntity> saved) {
    }

    private void validateMove(MiniAppScheduleChangeRequest request) {
        if (request.operation() == ScheduleChangeOperation.MOVE && request.newLessonDate() == null) {
            throw new MiniAppRequestException("Для переноса укажите новую дату");
        }
    }

    private ScheduleChangeEntity buildSnapshot(
            MiniAppScheduleChangeRequest request,
            ScheduleEntity source,
            ScheduleChangeEntity current,
            GroupEntity group,
            UserEntity author,
            String requestHash,
            long version,
            LocalDate targetDate,
            UUID childRequestId
    ) {
        LocalDate currentDate = current == null ? source.getLessonDate() : current.getNewLessonDate();
        var currentStart = current == null ? source.getStartTime() : current.getNewStartTime();
        var currentEnd = current == null ? source.getEndTime() : current.getNewEndTime();

        return ScheduleChangeEntity.builder()
                .occurrenceId(source.getOccurrenceId())
                .seriesId(ScheduleSeriesIdService.resolve(source))
                .clientRequestId(childRequestId)
                .batchRequestId(request.clientRequestId())
                .requestHash(requestHash)
                .operation(request.operation().name())
                .version(version)
                .group(group)
                .author(author)
                .subjectName(value(request.subjectName(), current == null ? source.getSubjectName() : current.getSubjectName()))
                .lessonType(value(request.lessonType(), current == null ? source.getLessonType().name() : current.getLessonType()))
                .teacherName(value(request.teacherName(), current == null ? teacherName(source) : current.getTeacherName()))
                .classroom(value(request.classroom(), current == null ? source.getClassroom() : current.getClassroom()))
                .description(value(request.description(), current == null ? null : current.getDescription()))
                .oldLessonDate(source.getLessonDate())
                .oldStartTime(source.getStartTime())
                .oldEndTime(source.getEndTime())
                .newLessonDate(targetDate)
                .newStartTime(request.newStartTime() == null ? currentStart : request.newStartTime())
                .newEndTime(request.newEndTime() == null ? currentEnd : request.newEndTime())
                .oldControlSum(source.getControlSum())
                .deleted(request.operation() == ScheduleChangeOperation.CANCEL)
                .build();
    }

    private void requireActualStarosta(UserEntity author, GroupEntity group) {
        if (author.getRole() != UserRole.STAROSTA
                || group.getStarosta() == null
                || !group.getStarosta().getId().equals(author.getId())) {
            throw forbidden();
        }
    }

    private ScheduleChangeForbiddenException forbidden() {
        return new ScheduleChangeForbiddenException(
                "Изменять расписание может только староста своей группы");
    }

    private String teacherName(ScheduleEntity source) {
        if (source.getTeacher() == null) {
            return null;
        }
        return String.join(" ",
                value(source.getTeacher().getLastName(), ""),
                value(source.getTeacher().getFirstName(), ""),
                value(source.getTeacher().getPatronymic(), "")
        ).trim();
    }

    private <T> T value(T requested, T fallback) {
        return requested == null ? fallback : requested;
    }

    private void evictDay(String groupName, LocalDate date) {
        if (date == null) {
            return;
        }
        Cache cache = cacheManager.getCache("scheduleDay");
        if (cache != null) {
            cache.evict(groupName + "-" + date.format(DateUtils.FORMATTER));
        }
    }

    private String requestHash(MiniAppScheduleChangeRequest request) {
        String canonical = String.join("|",
                request.lessonOccurrenceId().toString(),
                request.expectedVersion().toString(),
                request.operation().name(),
                request.scope().name(),
                Objects.toString(request.subjectName(), ""),
                Objects.toString(request.lessonType(), ""),
                Objects.toString(request.teacherName(), ""),
                Objects.toString(request.classroom(), ""),
                Objects.toString(request.description(), ""),
                Objects.toString(request.newLessonDate(), ""),
                Objects.toString(request.newStartTime(), ""),
                Objects.toString(request.newEndTime(), "")
        );
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not supported", exception);
        }
    }

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public ScheduleChangeEntity createOrUpdate(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleEntity scheduleEntity = scheduleRepository.findSchedule(
                group, dto.getOldLessonDate(), dto.getOldStartTime(),
                dto.getOldEndTime(), LessonType.valueOfString(dto.getLessonType()), dto.getSubjectName()
        );
        ScheduleChangeEntity entity = new ScheduleChangeEntity();

        entity.setGroup(group);
        entity.setSubjectName(dto.getSubjectName());
        entity.setLessonType(dto.getLessonType());
        entity.setTeacherName(dto.getTeacherName());
        entity.setClassroom(dto.getClassroom());

        entity.setOldLessonDate(dto.getOldLessonDate());
        entity.setOldStartTime(dto.getOldStartTime());
        entity.setOldEndTime(dto.getOldEndTime());
        entity.setNewStartTime(dto.getNewStartTime());
        entity.setNewEndTime(dto.getNewEndTime());
        entity.setNewLessonDate(dto.getNewLessonDate());
        entity.setOldControlSum(scheduleEntity.getControlSum());
        entity.setDescription(dto.getDescription());
        entity.setDeleted(false);

        return changeRepository.save(entity);
    }

    public ScheduleChangeForEveryDayCheckDTO getChangesDtoForDay(String groupName, LocalDate date) {
        GroupEntity group = groupService.findEntityByName(groupName);
        return changesToDto(changeRepository.findAllByGroupAndOldLessonDate(group, date));
    }

    public List<ScheduleChangeEntity> getChangesDtoAnyDay(String groupName, LocalDate date) {
        GroupEntity group = groupService.findEntityByName(groupName);
        Map<java.util.UUID, ScheduleChangeEntity> latest = new LinkedHashMap<>();
        List<ScheduleChangeEntity> legacy = new ArrayList<>();
        changeRepository.findAllByGroupOrderByIdAsc(group).forEach(change -> {
            if (change.getOccurrenceId() == null) {
                if (date.equals(change.getOldLessonDate()) || date.equals(change.getNewLessonDate())) {
                    legacy.add(change);
                }
            } else {
                latest.put(change.getOccurrenceId(), change);
            }
        });
        legacy.addAll(latest.values().stream()
                .filter(change -> date.equals(change.getOldLessonDate())
                        || date.equals(change.getNewLessonDate()))
                .toList());
        return List.copyOf(legacy);
    }

    private ScheduleChangeForEveryDayCheckDTO changesToDto(List<ScheduleChangeEntity> scheduleChangeEntities) {
        return ScheduleChangeForEveryDayCheckDTO.builder()
                .scheduleChangeEntityList(scheduleChangeEntities
                        .stream()
                        .map(this::prepareForFront)
                        .toList())
                .build();
    }

    private ScheduleChangeForFrontDTO prepareForFront(ScheduleChangeEntity scheduleChangeEntity) {
        return ScheduleChangeForFrontDTO.builder()
                .deleted(scheduleChangeEntity.isDeleted())
                .classroom(scheduleChangeEntity.getClassroom())
                .lessonType(scheduleChangeEntity.getLessonType())
                .newEndTime(scheduleChangeEntity.getNewEndTime())
                .newLessonDate(scheduleChangeEntity.getNewLessonDate())
                .newStartTime(scheduleChangeEntity.getNewStartTime())
                .oldEndTime(scheduleChangeEntity.getOldEndTime())
                .oldLessonDate(scheduleChangeEntity.getOldLessonDate())
                .oldStartTime(scheduleChangeEntity.getOldStartTime())
                .subjectName(scheduleChangeEntity.getSubjectName())
                .teacherName(scheduleChangeEntity.getTeacherName())
                .description(scheduleChangeEntity.getDescription())
                .build();
    }

    @CacheEvict(value = "scheduleDay", key = "#groupName + '-' + #dto.oldLessonDate.format(T(ru.semavin.telegrambot.utils.DateUtils).FORMATTER)")
    public void markAsDeleted(ScheduleChangeDTO dto, String groupName) {
        GroupEntity group = groupService.findEntityByName(groupName);
        ScheduleEntity scheduleEntity = scheduleRepository.findSchedule(
                group, dto.getOldLessonDate(), dto.getOldStartTime(),
                dto.getOldEndTime(), LessonType.valueOfString(dto.getLessonType()), dto.getSubjectName()
        );
        ScheduleChangeEntity entity = new ScheduleChangeEntity();

        entity.setGroup(group);
        entity.setSubjectName(dto.getSubjectName());
        entity.setLessonType(dto.getLessonType());
        entity.setTeacherName(dto.getTeacherName());
        entity.setClassroom(dto.getClassroom());

        entity.setOldLessonDate(dto.getOldLessonDate());
        entity.setOldStartTime(dto.getOldStartTime());
        entity.setOldEndTime(dto.getOldEndTime());

        entity.setNewLessonDate(dto.getNewLessonDate());
        entity.setNewStartTime(dto.getNewStartTime());
        entity.setNewEndTime(dto.getNewEndTime());
        entity.setOldControlSum(scheduleEntity.getControlSum());
        entity.setDescription(dto.getDescription());
        entity.setDeleted(true);

        changeRepository.save(entity);
    }

}
