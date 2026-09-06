package ru.semavin.telegrambot.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import ru.semavin.telegrambot.dto.MiniAppScheduleChangeRequest;
import ru.semavin.telegrambot.models.*;
import ru.semavin.telegrambot.models.enums.LessonType;
import ru.semavin.telegrambot.models.enums.ScheduleChangeOperation;
import ru.semavin.telegrambot.models.enums.ScheduleChangeScope;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.NotificationHistoryRepository;
import ru.semavin.telegrambot.repositories.ScheduleChangeRepository;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;
import ru.semavin.telegrambot.utils.exceptions.ScheduleChangeConflictException;
import ru.semavin.telegrambot.utils.exceptions.ScheduleChangeForbiddenException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ScheduleChangeServiceTest {
    private ScheduleChangeRepository changeRepository;
    private ScheduleRepository scheduleRepository;
    private UserService userService;
    private GroupRepository groupRepository;
    private NotificationHistoryRepository notificationHistoryRepository;
    private Cache cache;
    private ScheduleChangeService service;

    private UUID occurrenceId;
    private GroupEntity group;
    private UserEntity starosta;
    private ScheduleEntity source;

    @BeforeEach
    void setUp() {
        changeRepository = mock(ScheduleChangeRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        userService = mock(UserService.class);
        groupRepository = mock(GroupRepository.class);
        notificationHistoryRepository = mock(NotificationHistoryRepository.class);
        CacheManager cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        service = new ScheduleChangeService(
                changeRepository,
                scheduleRepository,
                mock(GroupService.class),
                userService,
                groupRepository,
                cacheManager,
                notificationHistoryRepository
        );

        occurrenceId = UUID.randomUUID();
        group = GroupEntity.builder().id(10L).groupName("GROUP-1").build();
        starosta = UserEntity.builder().id(1L).telegramId(100L)
                .role(UserRole.STAROSTA).group(group).build();
        group.setStarosta(starosta);
        group.getUsers().add(starosta);
        source = ScheduleEntity.builder()
                .id(20L)
                .occurrenceId(occurrenceId)
                .group(group)
                .subjectName("Math")
                .lessonType(LessonType.LECTURE)
                .classroom("101")
                .lessonDate(LocalDate.of(2026, 9, 1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .controlSum("source-sum")
                .build();

        when(userService.findById(1L)).thenReturn(starosta);
        when(groupRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(group));
        when(scheduleRepository.findByOccurrenceIdForUpdate(occurrenceId)).thenReturn(Optional.of(source));
        when(changeRepository.findFirstByOccurrenceIdOrderByVersionDesc(occurrenceId))
                .thenReturn(Optional.empty());
        when(changeRepository.findByGroupAndClientRequestId(any(), any())).thenReturn(Optional.empty());
        when(changeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(cacheManager.getCache("scheduleDay")).thenReturn(cache);
    }

    @Test
    void notifiesEveryEnabledGroupMemberRegardlessOfCurrentRole() {
        UserEntity formerStarosta = UserEntity.builder().id(2L).telegramId(200L)
                .role(UserRole.STUDENT).group(group).scheduleNotificationsEnabled(true).build();
        group.getUsers().add(formerStarosta);

        service.apply(request(UUID.randomUUID(), 0, ScheduleChangeOperation.UPDATE), 1L);

        verify(notificationHistoryRepository, times(2)).save(any(NotificationHistoryEntity.class));
        verify(notificationHistoryRepository).save(org.mockito.ArgumentMatchers.argThat(event ->
                event.getUser() == formerStarosta
                        && "SCHEDULE_CHANGE".equals(event.getType())
                        && "DISABLED".equals(event.getTelegramDeliveryStatus())));
    }

    @Test
    void rejectsStudentEvenWhenMembershipMatches() {
        starosta.setRole(UserRole.STUDENT);

        assertThrows(ScheduleChangeForbiddenException.class,
                () -> service.apply(request(UUID.randomUUID(), 0, ScheduleChangeOperation.UPDATE), 1L));

        verify(changeRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void moveCreatesAuthoredNextVersionAndEvictsBothDates() {
        UUID requestId = UUID.randomUUID();
        var response = service.apply(new MiniAppScheduleChangeRequest(
                occurrenceId, requestId, 0L, ScheduleChangeOperation.MOVE, ScheduleChangeScope.SINGLE,
                null, null, null, null, "Moved",
                LocalDate.of(2026, 9, 2), LocalTime.of(11, 0), LocalTime.of(12, 30)
        ), 1L);

        assertEquals(occurrenceId, response.lessonOccurrenceId());
        assertEquals(requestId, response.clientRequestId());
        assertEquals(1, response.version());
        verify(changeRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.argThat(changes -> {
            ScheduleChangeEntity change = changes.iterator().next();
            return change.getAuthor() == starosta
                    && change.getGroup() == group
                    && change.getVersion() == 1
                    && change.getOccurrenceId().equals(occurrenceId);
        }));
        verify(cache).evict("GROUP-1-01.09.2026");
        verify(cache).evict("GROUP-1-02.09.2026");
    }

    @Test
    void identicalClientRequestIsReplayedWithoutSecondInsert() {
        UUID requestId = UUID.randomUUID();
        MiniAppScheduleChangeRequest request = request(requestId, 0, ScheduleChangeOperation.UPDATE);
        AtomicReference<ScheduleChangeEntity> saved = new AtomicReference<>();
        when(changeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<ScheduleChangeEntity> changes = invocation.getArgument(0);
            ScheduleChangeEntity change = changes.getFirst();
            saved.set(change);
            return changes;
        });

        var first = service.apply(request, 1L);
        when(changeRepository.findByGroupAndClientRequestId(group, requestId))
                .thenAnswer(ignored -> Optional.of(saved.get()));
        var replay = service.apply(request, 1L);

        assertEquals(first, replay);
        verify(changeRepository, org.mockito.Mockito.times(1)).saveAllAndFlush(any());
    }

    @Test
    void staleVersionIsRejected() {
        ScheduleChangeEntity current = ScheduleChangeEntity.builder()
                .occurrenceId(occurrenceId)
                .version(2L)
                .newLessonDate(source.getLessonDate())
                .newStartTime(source.getStartTime())
                .newEndTime(source.getEndTime())
                .build();
        when(changeRepository.findFirstByOccurrenceIdOrderByVersionDesc(occurrenceId))
                .thenReturn(Optional.of(current));

        assertThrows(ScheduleChangeConflictException.class,
                () -> service.apply(request(UUID.randomUUID(), 1, ScheduleChangeOperation.UPDATE), 1L));
        verify(changeRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void cancelCreatesDeletedSnapshot() {
        service.apply(request(UUID.randomUUID(), 0, ScheduleChangeOperation.CANCEL), 1L);

        verify(changeRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.argThat(changes ->
                changes.iterator().next().isDeleted()
                        && "CANCEL".equals(changes.iterator().next().getOperation())));
    }

    @Test
    void reusedClientRequestWithDifferentPayloadIsRejected() {
        UUID requestId = UUID.randomUUID();
        MiniAppScheduleChangeRequest firstRequest = request(requestId, 0, ScheduleChangeOperation.UPDATE);
        AtomicReference<ScheduleChangeEntity> saved = new AtomicReference<>();
        when(changeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<ScheduleChangeEntity> changes = invocation.getArgument(0);
            ScheduleChangeEntity change = changes.getFirst();
            saved.set(change);
            return changes;
        });
        service.apply(firstRequest, 1L);
        when(changeRepository.findByGroupAndClientRequestId(group, requestId))
                .thenAnswer(ignored -> Optional.of(saved.get()));
        MiniAppScheduleChangeRequest conflicting = new MiniAppScheduleChangeRequest(
                occurrenceId, requestId, 0L, ScheduleChangeOperation.UPDATE, ScheduleChangeScope.SINGLE,
                "Another subject", null, null, null, null,
                null, null, null
        );

        assertThrows(ScheduleChangeConflictException.class,
                () -> service.apply(conflicting, 1L));
        verify(changeRepository, org.mockito.Mockito.times(1)).saveAllAndFlush(any());
    }

    @Test
    void seriesMoveShiftsEveryFutureOccurrenceWithoutCollapsingDates() {
        UUID seriesId = UUID.randomUUID();
        source.setSeriesId(seriesId);
        ScheduleEntity nextWeek = lesson(
                UUID.randomUUID(), seriesId, LocalDate.of(2026, 9, 8));
        when(scheduleRepository.findAllByGroup(group)).thenReturn(List.of(source, nextWeek));
        UUID requestId = UUID.randomUUID();
        MiniAppScheduleChangeRequest request = new MiniAppScheduleChangeRequest(
                occurrenceId, requestId, 0L, ScheduleChangeOperation.MOVE, ScheduleChangeScope.SERIES,
                null, null, null, null, null,
                LocalDate.of(2026, 9, 3), LocalTime.of(11, 0), LocalTime.of(12, 30)
        );

        service.apply(request, 1L);

        verify(changeRepository).saveAllAndFlush(org.mockito.ArgumentMatchers.argThat(changes -> {
            List<ScheduleChangeEntity> result = new java.util.ArrayList<>();
            changes.forEach(result::add);
            return result.size() == 2
                    && result.get(0).getNewLessonDate().equals(LocalDate.of(2026, 9, 3))
                    && result.get(1).getNewLessonDate().equals(LocalDate.of(2026, 9, 10))
                    && result.stream().allMatch(change -> requestId.equals(change.getBatchRequestId()))
                    && result.stream().map(ScheduleChangeEntity::getClientRequestId).distinct().count() == 2;
        }));
    }

    @Test
    void movedSeriesOccurrenceCanBeEditedIndividuallyAfterwards() {
        UUID seriesId = UUID.randomUUID();
        source.setSeriesId(seriesId);
        ScheduleEntity nextWeek = lesson(
                UUID.randomUUID(), seriesId, LocalDate.of(2026, 9, 8));
        when(scheduleRepository.findAllByGroup(group)).thenReturn(List.of(source, nextWeek));
        MiniAppScheduleChangeRequest seriesMove = new MiniAppScheduleChangeRequest(
                occurrenceId, UUID.randomUUID(), 0L,
                ScheduleChangeOperation.MOVE, ScheduleChangeScope.SERIES,
                null, null, null, null, null,
                LocalDate.of(2026, 9, 3), null, null
        );
        AtomicReference<List<ScheduleChangeEntity>> seriesSaved = new AtomicReference<>();
        when(changeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<ScheduleChangeEntity> changes = invocation.getArgument(0);
            seriesSaved.set(changes);
            return changes;
        });
        service.apply(seriesMove, 1L);
        ScheduleChangeEntity movedNextWeek = seriesSaved.get().stream()
                .filter(change -> change.getOccurrenceId().equals(nextWeek.getOccurrenceId()))
                .findFirst().orElseThrow();
        when(scheduleRepository.findByOccurrenceIdForUpdate(nextWeek.getOccurrenceId()))
                .thenReturn(Optional.of(nextWeek));
        when(changeRepository.findFirstByOccurrenceIdOrderByVersionDesc(nextWeek.getOccurrenceId()))
                .thenReturn(Optional.of(movedNextWeek));
        MiniAppScheduleChangeRequest individual = new MiniAppScheduleChangeRequest(
                nextWeek.getOccurrenceId(), UUID.randomUUID(), 1L,
                ScheduleChangeOperation.UPDATE, ScheduleChangeScope.SINGLE,
                null, null, "Новый преподаватель", null, null,
                null, null, null
        );

        service.apply(individual, 1L);

        List<ScheduleChangeEntity> individualSaved = seriesSaved.get();
        assertThat(individualSaved).singleElement().satisfies(change -> {
            assertThat(change.getOccurrenceId()).isEqualTo(nextWeek.getOccurrenceId());
            assertThat(change.getVersion()).isEqualTo(2);
            assertThat(change.getNewLessonDate()).isEqualTo(LocalDate.of(2026, 9, 10));
            assertThat(change.getTeacherName()).isEqualTo("Новый преподаватель");
        });
    }

    @Test
    void seriesBatchReplayDoesNotCreateMoreChildChanges() {
        UUID seriesId = UUID.randomUUID();
        source.setSeriesId(seriesId);
        ScheduleEntity nextWeek = lesson(UUID.randomUUID(), seriesId, LocalDate.of(2026, 9, 8));
        when(scheduleRepository.findAllByGroup(group)).thenReturn(List.of(source, nextWeek));
        UUID requestId = UUID.randomUUID();
        MiniAppScheduleChangeRequest request = new MiniAppScheduleChangeRequest(
                occurrenceId, requestId, 0L,
                ScheduleChangeOperation.UPDATE, ScheduleChangeScope.SERIES,
                null, null, "Преподаватель", null, null,
                null, null, null
        );
        AtomicReference<List<ScheduleChangeEntity>> saved = new AtomicReference<>();
        when(changeRepository.saveAllAndFlush(any())).thenAnswer(invocation -> {
            List<ScheduleChangeEntity> changes = invocation.getArgument(0);
            saved.set(changes);
            return changes;
        });
        service.apply(request, 1L);
        when(changeRepository.findAllByGroupAndBatchRequestIdOrderByIdAsc(group, requestId))
                .thenAnswer(ignored -> saved.get());

        service.apply(request, 1L);

        verify(changeRepository, org.mockito.Mockito.times(1)).saveAllAndFlush(any());
    }

    private ScheduleEntity lesson(UUID id, UUID seriesId, LocalDate date) {
        return source.toBuilder()
                .id(source.getId() + 1)
                .occurrenceId(id)
                .seriesId(seriesId)
                .lessonDate(date)
                .build();
    }

    private MiniAppScheduleChangeRequest request(
            UUID requestId,
            long version,
            ScheduleChangeOperation operation
    ) {
        return new MiniAppScheduleChangeRequest(
                occurrenceId, requestId, version, operation, ScheduleChangeScope.SINGLE,
                "Updated Math", null, null, null, null,
                null, null, null
        );
    }
}
