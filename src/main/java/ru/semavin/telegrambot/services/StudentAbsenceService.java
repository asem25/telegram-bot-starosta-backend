package ru.semavin.telegrambot.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.semavin.telegrambot.dto.GroupStudentAbsenceResponse;
import ru.semavin.telegrambot.dto.StudentAbsenceRequest;
import ru.semavin.telegrambot.dto.StudentAbsenceResponse;
import ru.semavin.telegrambot.models.StudentAbsenceEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.StudentAbsenceRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.AbsenceForbiddenException;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRequestException;
import ru.semavin.telegrambot.utils.exceptions.UserNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentAbsenceService {
    private final StudentAbsenceRepository absenceRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<StudentAbsenceResponse> getOwn(Long userId) {
        requireUser(userId);
        return absenceRepository.findAllByUserIdOrderByStartDateDesc(userId).stream()
                .map(StudentAbsenceResponse::from)
                .toList();
    }

    @Transactional
    public StudentAbsenceResponse create(Long userId, StudentAbsenceRequest request) {
        UserEntity user = requireUser(userId);
        requireStudent(user);
        if (request.endDate().isBefore(request.startDate())) {
            throw new MiniAppRequestException("Дата окончания отсутствия раньше даты начала");
        }

        StudentAbsenceEntity saved = absenceRepository.save(StudentAbsenceEntity.builder()
                .user(user)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(normalizeReason(request.reason()))
                .build());
        return StudentAbsenceResponse.from(saved);
    }

    @Transactional
    public void deleteOwn(Long userId, Long absenceId) {
        requireUser(userId);
        if (absenceRepository.deleteOwned(absenceId, userId) == 0) {
            throw new AbsenceForbiddenException("Можно удалить только собственный диапазон отсутствия");
        }
    }

    @Transactional(readOnly = true)
    public List<GroupStudentAbsenceResponse> getOwnGroup(Long userId) {
        UserEntity starosta = requireUser(userId);
        if (starosta.getGroup() == null
                || starosta.getGroup().getStarosta() == null
                || !starosta.getId().equals(starosta.getGroup().getStarosta().getId())) {
            throw new AbsenceForbiddenException("Отсутствия группы доступны только её старосте");
        }
        return absenceRepository.findAllByUserGroupIdOrderByStartDateDesc(starosta.getGroup().getId()).stream()
                .map(GroupStudentAbsenceResponse::from)
                .toList();
    }

    private UserEntity requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ExceptionFabric.create(
                        UserNotFoundException.class,
                        ExceptionMessages.USER_NOT_FOUND));
    }

    private void requireStudent(UserEntity user) {
        if (user.getRole() != UserRole.STUDENT && user.getRole() != UserRole.STAROSTA) {
            throw new AbsenceForbiddenException("Диапазоны отсутствия доступны только студентам");
        }
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        return reason.trim();
    }
}
