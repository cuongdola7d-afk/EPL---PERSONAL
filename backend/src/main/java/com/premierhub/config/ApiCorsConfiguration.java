package com.premierhub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class ApiCorsConfiguration implements WebMvcConfigurer {
    private final List<String> allowedOrigins;

    public ApiCorsConfiguration(@Value("${PREMIERHUB_CORS_ALLOWED_ORIGINS:}") String configuredOrigins) {
        allowedOrigins = new ArrayList<>(List.of("http://localhost:5173", "http://127.0.0.1:5173"));

        for (String value : configuredOrigins.split(",")) {
            if (!value.isBlank()) {
                allowedOrigins.add(normalizeOrigin(value));
            }
        }
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET")
                .allowedHeaders("Accept", "Content-Type");
    }

    private static String normalizeOrigin(String value) {
        String origin = value.trim().replaceAll("/+$", "");
        URI uri = URI.create(origin);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty())
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("CORS origin must be an exact http(s) origin: " + value);
        }
        return origin;
    }
}
