package ru.semavin.telegrambot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.semavin.telegrambot.dto.RequestDTO;
import ru.semavin.telegrambot.models.enums.ExceptionMessages;
import ru.semavin.telegrambot.services.RequestService;
import ru.semavin.telegrambot.utils.ExceptionFabric;
import ru.semavin.telegrambot.utils.exceptions.KeyNotEqualsException;

@RestController
@RequestMapping("api/v1/requests")
@SecurityRequirement(name = "API-KEY")
@RequiredArgsConstructor
@Slf4j
public class RequestController {
    private final RequestService requestService;
    //TODO Delete запрос на принятие, get на получение списка
    @Value("${key.api}")
    private String keyApi;
    @PostMapping("")
    @Operation(
            summary = "Сохранение заявки в группу",
            description = "Сохраняет заявку в группу.",
            security = @SecurityRequirement(name = "API-KEY"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Запрос сохранен"),
                    @ApiResponse(responseCode = "404", description = "Староста/Группа не найден"),
                    @ApiResponse(responseCode = "403", description = "API-KEY невалиден"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            }
    )
    public ResponseEntity<RequestDTO> saveRequest(@RequestBody RequestDTO requestDTO,
                                                  @RequestHeader("API-KEY") String apiKey) {
        if (!keyApi.equals(apiKey)) {
            log.warn("API key in header not equals");
            throw ExceptionFabric.create(KeyNotEqualsException.class, ExceptionMessages.KEY_NOT_VALID);
        }
        log.info("Пришел запрос POST /requests");
        requestService.save(requestDTO);
        log.info("Запрос успешен POST /requests");
        return ResponseEntity.ok(requestDTO);
    }

}
