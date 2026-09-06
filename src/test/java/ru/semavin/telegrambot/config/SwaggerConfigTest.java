package ru.semavin.telegrambot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SwaggerConfigTest {

    @Test
    void declaresOpaqueBearerAuthenticationScheme() {
        OpenAPI openAPI = new SwaggerConfig().customOpenAPI();

        SecurityScheme bearer = openAPI.getComponents()
                .getSecuritySchemes()
                .get("Bearer Authentication");

        assertEquals(SecurityScheme.Type.HTTP, bearer.getType());
        assertEquals("bearer", bearer.getScheme());
        assertNull(bearer.getBearerFormat());
        assertFalse(openAPI.getComponents().getSecuritySchemes().containsKey("API-KEY"));
        assertNull(openAPI.getSecurity());
    }
}
