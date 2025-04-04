package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO для пользователя.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для пользователя в системе")
public class UserDTO {

    @NotNull(message = "Telegram ID не может быть null")
    @Schema(description = "Идентификатор пользователя в Telegram", example = "123456789")
    private Long telegramId;

    @NotNull(message = "Тэг пользователя не может быть null")
    @Size(min = 3, max = 50, message = "Тэг должно быть от 3 до 50 символов")
    @Schema(description = "Тэг пользователя в телеграмм", example = "john_doe")
    private String username;

    @Schema(description = "Имя пользователя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "Иванов")
    private String lastName;
    @Schema(description = "Отчество пользователя", example = "Иванович")
    private String patronymic;
    @Builder.Default
    @Schema(description = "Роль пользователя", example = "user")
    private String role = "STUDENT";


    @Size(min = 3, max = 16, message = "Название группы должно быть от 3 до 16 символов")
    @Schema(description = "Название группы", example = "М3О-303С-22")
    private String groupName;
}
