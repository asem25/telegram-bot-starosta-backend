package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.GroupDTO;
import ru.semavin.telegrambot.services.GroupService;

@RestController
@RequestMapping("api/v1/groups")
@SecurityRequirement(name = "API-KEY")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Group Controller", description = "Контроллер для управления группами")
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    @Operation(
            summary = "Создать группу",
            description = "Создаёт новую группу и возвращает DTO созданной группы.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Группа успешно создана"),
                    @ApiResponse(responseCode = "400", description = "Некорректные данные запроса"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            }
    )
    public ResponseEntity<GroupDTO> createGroup(@RequestBody GroupDTO groupDTO) {
        return ResponseEntity.ok(groupService.createGroup(groupDTO));
    }

    @GetMapping
    @Operation(
            summary = "Получить данные группы",
            description = "Возвращает DTO группы по её названию.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Группа найдена"),
                    @ApiResponse(responseCode = "404", description = "Группа не найдена")
            }
    )
    public ResponseEntity<GroupDTO> getGroup(
            @Parameter(description = "Название группы для поиска", example = "М3О-303С-22")
            @RequestParam String groupName
    ) {
        log.info("Mapping to /group GET {}", groupName);
        GroupDTO groupDTO = groupService.findDtoByName(groupName);
        log.info("Find group: {}", groupDTO);
        return ResponseEntity.ok(groupDTO);
    }

    @PatchMapping
    @Operation(
            summary = "Назначить старосту группе",
            description = "Обновляет группу, устанавливая указанного пользователя как старосту.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Группа успешно обновлена"),
                    @ApiResponse(responseCode = "404", description = "Пользователь или группа не найдены")
            }
    )
    public ResponseEntity<GroupDTO> updateGroup(
            @Parameter(description = "Название группы, которую нужно обновить", example = "М3О-303С-22")
            @RequestParam String groupName,
            @Parameter(description = "Никнейм пользователя, которого нужно назначить старостой", example = "john_doe")
            @RequestParam String starostaUsername
    ) {
        log.info("Mapping to /group PATCH; group:{}, username:{}", groupName, starostaUsername);
        GroupDTO updatedGroupDTO = groupService.setStarosta(groupName, starostaUsername);
        log.info("Update group: {}", groupName);
        return ResponseEntity.ok(updatedGroupDTO);
    }
}
