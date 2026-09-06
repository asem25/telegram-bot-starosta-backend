package ru.semavin.telegrambot.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.semavin.telegrambot.dto.StudentAbsenceRequest;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.StudentAbsenceEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.StudentAbsenceRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.exceptions.AbsenceForbiddenException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentAbsenceServiceTest {
    private StudentAbsenceRepository absenceRepository;
    private UserRepository userRepository;
    private StudentAbsenceService service;

    @BeforeEach
    void setUp() {
        absenceRepository = mock(StudentAbsenceRepository.class);
        userRepository = mock(UserRepository.class);
        service = new StudentAbsenceService(absenceRepository, userRepository);
    }

    @Test
    void createsLocalDateRangeForBearerStudentAndNormalizesEmptyReason() {
        UserEntity student = UserEntity.builder()
                .id(1L).telegramId(100L).role(UserRole.STUDENT).build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(student));
        when(absenceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.create(100L, new StudentAbsenceRequest(
                LocalDate.of(2026, 9, 2),
                LocalDate.of(2026, 9, 4),
                "  "));

        assertEquals(LocalDate.of(2026, 9, 2), response.startDate());
        assertEquals(LocalDate.of(2026, 9, 4), response.endDate());
        assertNull(response.reason());
    }

    @Test
    void rejectsReversedRangeBeforePersistence() {
        UserEntity student = UserEntity.builder()
                .id(1L).telegramId(100L).role(UserRole.STUDENT).build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(student));

        assertThrows(MiniAppRequestException.class, () -> service.create(100L,
                new StudentAbsenceRequest(LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 2), null)));

        verify(absenceRepository, never()).save(any());
    }

    @Test
    void deletionIsScopedToAuthenticatedOwner() {
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(UserEntity.builder().build()));
        when(absenceRepository.deleteOwned(9L, 100L)).thenReturn(0);

        assertThrows(AbsenceForbiddenException.class, () -> service.deleteOwn(100L, 9L));

        verify(absenceRepository).deleteOwned(9L, 100L);
    }

    @Test
    void starostaReadsOnlyAbsencesSelectedByOwnGroupId() {
        UserEntity starosta = UserEntity.builder().id(1L).telegramId(100L).role(UserRole.STAROSTA).build();
        GroupEntity ownGroup = GroupEntity.builder().id(10L).starosta(starosta).build();
        starosta.setGroup(ownGroup);
        UserEntity member = UserEntity.builder().id(2L).firstName("Студент").group(ownGroup).build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(starosta));
        when(absenceRepository.findAllByUserGroupIdOrderByStartDateDesc(10L)).thenReturn(List.of(
                StudentAbsenceEntity.builder()
                        .id(3L).user(member)
                        .startDate(LocalDate.of(2026, 9, 2))
                        .endDate(LocalDate.of(2026, 9, 2))
                        .build()));

        assertEquals(1, service.getOwnGroup(100L).size());

        verify(absenceRepository).findAllByUserGroupIdOrderByStartDateDesc(10L);
    }

    @Test
    void roleFlagAloneCannotReadAnotherGroupsAbsences() {
        UserEntity anotherStarosta = UserEntity.builder().id(2L).build();
        GroupEntity group = GroupEntity.builder().id(10L).starosta(anotherStarosta).build();
        UserEntity caller = UserEntity.builder()
                .id(1L).telegramId(100L).role(UserRole.STAROSTA).group(group).build();
        when(userRepository.findByTelegramId(100L)).thenReturn(Optional.of(caller));

        assertThrows(AbsenceForbiddenException.class, () -> service.getOwnGroup(100L));

        verify(absenceRepository, never()).findAllByUserGroupIdOrderByStartDateDesc(any());
    }
}
