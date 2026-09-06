package ru.semavin.telegrambot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Value("${mini-app.allowed-origins:http://localhost:3000,http://localhost:3001,http://localhost:5173}")
    private String[] allowedOrigins;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/v1/auth/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("POST", "OPTIONS")
                        .allowedHeaders("Content-Type");
                registry.addMapping("/api/v1/me/**")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type");
                registry.addMapping("/api/v1/users/me")
                        .allowedOrigins(allowedOrigins)
                        .allowedMethods("PATCH", "OPTIONS")
                        .allowedHeaders("Authorization", "Content-Type");
            }
        };
    }
}
