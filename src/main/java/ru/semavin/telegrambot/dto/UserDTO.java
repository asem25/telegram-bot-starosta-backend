package ru.semavin.telegrambot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @NotNull(message = "Имя пользователя не может быть null")
    @Size(min = 3, max = 50, message = "Имя пользователя должно быть от 3 до 50 символов")
    @Schema(description = "Имя пользователя в Telegram", example = "john_doe")
    private String username;

    @Builder.Default
    @NotNull(message = "Роль пользователя не может быть null")
    @Size(min = 3, max = 20, message = "Роль пользователя должна быть от 3 до 20 символов")
    @Schema(description = "Роль пользователя", example = "user")
    private String role = "STUDENT";

    @NotNull(message = "Название группы не может быть null")
    @Size(min = 3, max = 50, message = "Название группы должно быть от 3 до 50 символов")
    @Schema(description = "Название группы", example = "М3О-303С-22")
    private String groupName;
}
