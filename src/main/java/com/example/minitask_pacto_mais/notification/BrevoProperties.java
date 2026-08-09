package com.example.minitask_pacto_mais.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.otp.brevo")
public record BrevoProperties(
        String apiKey,
        String senderEmail,
        String senderName
) {
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank()
                && senderEmail != null && !senderEmail.isBlank();
    }
}
