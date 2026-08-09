package com.example.minitask_pacto_mais.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.bot")
public record BotProperties(String apiKey) {

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
