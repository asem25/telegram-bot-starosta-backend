package ru.semavin.telegrambot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.semavin.telegrambot.dto.MiniAppGroupRequest;
import ru.semavin.telegrambot.dto.UserDTO;
import ru.semavin.telegrambot.services.UserService;
import ru.semavin.telegrambot.services.auth.AccessTokenService;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserControllerTest {
    private UserService userService;
    private AccessTokenService accessTokenService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        accessTokenService = mock(AccessTokenService.class);
        controller = new UserController(userService, accessTokenService);
    }

    @Test
    void selectsOwnGroupForBearerIdentityOnly() {
        UserDTO expected = UserDTO.builder()
                .groupName("М3О-503С-22")
                .build();
        when(accessTokenService.requireUserId("Bearer token")).thenReturn(73L);
        when(userService.assignInitialGroup(73L, "М3О-503С-22")).thenReturn(expected);

        UserDTO body = controller.selectOwnGroup(
                "Bearer token",
                new MiniAppGroupRequest("М3О-503С-22")).getBody();

        assertEquals(expected, body);
        verify(accessTokenService).requireUserId("Bearer token");
        verify(userService).assignInitialGroup(73L, "М3О-503С-22");
    }

    @Test
    void exposesGroupSelectionAtUsersMePath() throws NoSuchMethodException {
        RequestMapping controllerMapping = UserController.class.getAnnotation(RequestMapping.class);
        PatchMapping methodMapping = UserController.class
                .getMethod("selectOwnGroup", String.class, MiniAppGroupRequest.class)
                .getAnnotation(PatchMapping.class);

        assertEquals("api/v1/users", controllerMapping.value()[0]);
        assertEquals("/me", methodMapping.value()[0]);
    }

    @Test
    void exposesNoLegacyUserOperations() {
        List<Method> requestMethods = Arrays.stream(UserController.class.getDeclaredMethods())
                .filter(method -> Arrays.stream(method.getDeclaredAnnotations())
                        .anyMatch(annotation -> annotation.annotationType().getSimpleName().endsWith("Mapping")))
                .toList();

        assertEquals(List.of("selectOwnGroup"), requestMethods.stream().map(Method::getName).toList());
        assertEquals("/me", requestMethods.getFirst().getAnnotation(PatchMapping.class).value()[0]);
    }
}
