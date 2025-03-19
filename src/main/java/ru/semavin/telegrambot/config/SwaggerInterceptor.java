package ru.semavin.telegrambot.config;


import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SwaggerInterceptor implements HandlerInterceptor {

    @Value("${key.api}")
    private String apiKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestUri = request.getRequestURI();

        if (requestUri.startsWith("/swagger-ui") || requestUri.startsWith("/v3/api-docs")) {
            return true;
        }

        String requestApiKey = request.getHeader("API-KEY");
        if (requestApiKey == null || !requestApiKey.equals(apiKey)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Swagger access denied: Invalid API Key");
            return false;
        }
        return true;
    }
}
