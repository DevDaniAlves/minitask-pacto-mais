package com.example.minitask_pacto_mais.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BrevoOtpSender {

    private final BrevoProperties properties;
    private final RestClient.Builder restClientBuilder;

    public void sendEmail(String destination, String message) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Brevo não configurado");
        }

        String senderName = properties.senderName() == null || properties.senderName().isBlank()
                ? "Mini Task"
                : properties.senderName();

        Map<String, Object> body = Map.of(
                "sender", Map.of(
                        "name", senderName,
                        "email", properties.senderEmail()
                ),
                "to", List.of(Map.of("email", destination)),
                "subject", "Código Mini Task",
                "textContent", message
        );

        restClientBuilder.build()
                .post()
                .uri("https://api.brevo.com/v3/smtp/email")
                .contentType(MediaType.APPLICATION_JSON)
                .header("accept", "application/json")
                .header("api-key", properties.apiKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
