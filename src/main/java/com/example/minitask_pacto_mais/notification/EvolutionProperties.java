package com.example.minitask_pacto_mais.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp.evolution")
public record EvolutionProperties(
        String baseUrl,
        String apiKey,
        String instance
) {
    public boolean isServerConfigured() {
        return baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank();
    }

    public boolean isConfigured() {
        return isServerConfigured()
                && instance != null && !instance.isBlank();
    }
}
