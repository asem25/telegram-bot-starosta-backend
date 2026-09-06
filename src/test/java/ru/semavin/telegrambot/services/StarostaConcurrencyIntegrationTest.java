package ru.semavin.telegrambot.services;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.semavin.telegrambot.models.GroupEntity;
import ru.semavin.telegrambot.models.UserEntity;
import ru.semavin.telegrambot.models.enums.UserRole;
import ru.semavin.telegrambot.repositories.GroupRepository;
import ru.semavin.telegrambot.repositories.UserRepository;
import ru.semavin.telegrambot.utils.exceptions.MiniAppRoleConflictException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
class StarostaConcurrencyIntegrationTest {
    @Autowired
    private StarostaService starostaService;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long groupId;
    private Long foreignGroupId;
    private Long firstUserId;
    private Long secondUserId;

    @AfterEach
    void cleanOwnFixtures() {
        if (groupId != null) {
            jdbcTemplate.update("UPDATE groups SET starosta_id = NULL WHERE id = ?", groupId);
        }
        if (foreignGroupId != null) {
            jdbcTemplate.update("UPDATE groups SET starosta_id = NULL WHERE id = ?", foreignGroupId);
        }
        if (firstUserId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", firstUserId);
        }
        if (secondUserId != null) {
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", secondUserId);
        }
        if (groupId != null) {
            jdbcTemplate.update("DELETE FROM groups WHERE id = ?", groupId);
        }
        if (foreignGroupId != null) {
            jdbcTemplate.update("DELETE FROM groups WHERE id = ?", foreignGroupId);
        }
    }

    @Test
    void twoConcurrentClaimsChooseExactlyOneStarosta() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long telegramIdBase = Math.abs(UUID.randomUUID().getMostSignificantBits() / 4);
        GroupEntity group = groupRepository.saveAndFlush(
                GroupEntity.builder().groupName("RACE-" + suffix).build());
        groupId = group.getId();

        UserEntity first = userRepository.saveAndFlush(UserEntity.builder()
                .telegramId(telegramIdBase)
                .username("race_a_" + suffix)
                .role(UserRole.STUDENT)
                .group(group)
                .build());
        UserEntity second = userRepository.saveAndFlush(UserEntity.builder()
                .telegramId(telegramIdBase + 1)
                .username("race_b_" + suffix)
                .role(UserRole.STUDENT)
                .group(group)
                .build());
        firstUserId = first.getId();
        secondUserId = second.getId();

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstClaim = executor.submit(
                    () -> claimAfterSignal(first.getTelegramId(), ready, start));
            Future<Boolean> secondClaim = executor.submit(
                    () -> claimAfterSignal(second.getTelegramId(), ready, start));
            ready.await();
            start.countDown();

            assertEquals(1, List.of(firstClaim.get(), secondClaim.get()).stream()
                    .filter(Boolean::booleanValue)
                    .count());
        }

        GroupEntity storedGroup = groupRepository.findById(groupId).orElseThrow();
        assertNotNull(storedGroup.getStarosta());
        assertEquals(1, userRepository.findAllById(List.of(firstUserId, secondUserId)).stream()
                .filter(user -> user.getRole() == UserRole.STAROSTA)
                .count());
    }

    @Test
    void claimCanOnlyAffectAuthenticatedUsersOwnGroup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        long telegramId = Math.abs(UUID.randomUUID().getMostSignificantBits() / 4);
        GroupEntity ownGroup = groupRepository.saveAndFlush(
                GroupEntity.builder().groupName("OWN-" + suffix).build());
        GroupEntity foreignGroup = groupRepository.saveAndFlush(
                GroupEntity.builder().groupName("OTHER-" + suffix).build());
        groupId = ownGroup.getId();
        foreignGroupId = foreignGroup.getId();
        UserEntity user = userRepository.saveAndFlush(UserEntity.builder()
                .telegramId(telegramId)
                .username("own_" + suffix)
                .role(UserRole.STUDENT)
                .group(ownGroup)
                .build());
        firstUserId = user.getId();

        starostaService.claimOwnGroup(telegramId);

        assertEquals(user.getId(), groupRepository.findById(groupId).orElseThrow().getStarosta().getId());
        assertNull(groupRepository.findById(foreignGroupId).orElseThrow().getStarosta());
    }

    private boolean claimAfterSignal(Long telegramId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            starostaService.claimOwnGroup(telegramId);
            return true;
        } catch (MiniAppRoleConflictException expectedRaceLoss) {
            return false;
        }
    }
}
