package com.example.minitask_pacto_mais.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.frontend")
public record FrontendProperties(String baseUrl) {

    public String baseUrlOrDefault() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
