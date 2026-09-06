package ru.semavin.telegrambot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MiniAppGroupRequest(
        @NotBlank(message = "Название группы не может быть пустым")
        @Size(max = 20, message = "Название группы не может быть длиннее 20 символов")
        @Pattern(
                regexp = "^М\\d{1,2}О-\\d{3}[А-ЯЁ]{1,2}-\\d{2}$",
                message = "Название группы должно быть в формате М3О-503С-22"
        )
        String groupName
) {
}
