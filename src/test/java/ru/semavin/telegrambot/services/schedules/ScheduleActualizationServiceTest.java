package ru.semavin.telegrambot.services.schedules;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.semavin.telegrambot.mapper.ScheduleMapper;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.ScheduleEntity;
import ru.semavin.telegrambot.repositories.ScheduleRepository;
import ru.semavin.telegrambot.services.groups.GroupService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class ScheduleActualizationServiceTest {
    @Test
    void reconciliationCreatesNewListEntriesAndPreservesIdentity() {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        ScheduleParserService parser = mock(ScheduleParserService.class);
        GroupService groupService = mock(GroupService.class);
        GroupEntity group = GroupEntity.builder().id(1L).groupName("GROUP-1").build();
        UUID occurrenceId = UUID.randomUUID();
        UUID seriesId = UUID.randomUUID();
        ScheduleEntity existing = lesson(group).toBuilder()
                .id(10L).occurrenceId(occurrenceId).seriesId(seriesId).build();
        ScheduleEntity parsed = lesson(group);
        when(groupService.findEntityByName("GROUP-1")).thenReturn(group);
        when(repository.findAllByGroup(group)).thenReturn(List.of(existing));
        when(parser.findScheduleByGroup(group)).thenReturn(List.of(parsed));
        ScheduleActualizationService service = new ScheduleActualizationService(
                repository, parser, mock(ScheduleMapper.class), groupService,
                mock(ScheduleSyncStatusService.class));

        service.actualizationScheduleGroup("GROUP-1");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<ScheduleEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAllAndFlush(captor.capture());
        ScheduleEntity reconciled = captor.getValue().iterator().next();
        assertNotSame(parsed, reconciled);
        assertSame(occurrenceId, reconciled.getOccurrenceId());
        assertSame(seriesId, reconciled.getSeriesId());
    }

    @Test
    void recordsSyncOnlyAfterScheduleWasSavedSuccessfully() {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        ScheduleParserService parser = mock(ScheduleParserService.class);
        GroupService groupService = mock(GroupService.class);
        ScheduleSyncStatusService syncStatusService = mock(ScheduleSyncStatusService.class);
        GroupEntity group = GroupEntity.builder().id(1L).groupName("GROUP-1").build();
        when(groupService.findEntityByName("GROUP-1")).thenReturn(group);
        when(repository.findAllByGroup(group)).thenReturn(List.of());
        when(parser.findScheduleByGroup(group)).thenReturn(List.of(lesson(group)));
        ScheduleActualizationService service = new ScheduleActualizationService(
                repository, parser, mock(ScheduleMapper.class), groupService, syncStatusService);

        service.actualizationScheduleGroup("GROUP-1");

        verify(syncStatusService).markSuccessfulSync("GROUP-1");
    }

    @Test
    void doesNotRecordSyncWhenSavingFails() {
        ScheduleRepository repository = mock(ScheduleRepository.class);
        ScheduleParserService parser = mock(ScheduleParserService.class);
        GroupService groupService = mock(GroupService.class);
        ScheduleSyncStatusService syncStatusService = mock(ScheduleSyncStatusService.class);
        GroupEntity group = GroupEntity.builder().id(1L).groupName("GROUP-1").build();
        when(groupService.findEntityByName("GROUP-1")).thenReturn(group);
        when(repository.findAllByGroup(group)).thenReturn(List.of());
        when(parser.findScheduleByGroup(group)).thenReturn(List.of(lesson(group)));
        doThrow(new RuntimeException("save failed")).when(repository).saveAllAndFlush(org.mockito.ArgumentMatchers.any());
        ScheduleActualizationService service = new ScheduleActualizationService(
                repository, parser, mock(ScheduleMapper.class), groupService, syncStatusService);

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> service.actualizationScheduleGroup("GROUP-1"));

        verify(syncStatusService, never()).markSuccessfulSync("GROUP-1");
    }

    private ScheduleEntity lesson(GroupEntity group) {
        return ScheduleEntity.builder()
                .group(group)
                .subjectName("Math")
                .lessonDate(LocalDate.of(2026, 9, 1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 30))
                .controlSum("same")
                .build();
    }
}
