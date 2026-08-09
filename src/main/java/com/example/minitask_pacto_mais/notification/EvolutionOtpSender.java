package com.example.minitask_pacto_mais.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class EvolutionOtpSender {

    private final EvolutionProperties properties;
    private final RestClient.Builder restClientBuilder;

    public void sendWhatsApp(String destination, String message) {
        if (!properties.isConfigured()) {
            throw new IllegalStateException("Evolution API não configurada");
        }

        String number = destination == null ? "" : destination.replaceAll("\\D", "");
        if (number.isBlank()) {
            throw new IllegalArgumentException("Telefone inválido para WhatsApp");
        }

        String uri = UriComponentsBuilder
                .fromUriString(trimTrailingSlash(properties.baseUrl()))
                .pathSegment("message", "sendText", properties.instance())
                .build()
                .toUriString();

        Map<String, Object> body = Map.of(
                "number", number,
                "text", message
        );

        restClientBuilder.build()
                .post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .header("apikey", properties.apiKey())
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
