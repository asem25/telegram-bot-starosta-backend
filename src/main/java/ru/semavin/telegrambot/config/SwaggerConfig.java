package ru.semavin.telegrambot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Telegram Bot API")
                        .version("1.0")
                        .description("API для получения расписания и управления ботом"))
                .addSecurityItem(new SecurityRequirement().addList("API-KEY"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("API-KEY",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("API-KEY")));
    }
}