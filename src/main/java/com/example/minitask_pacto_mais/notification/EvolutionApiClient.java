package com.example.minitask_pacto_mais.notification;

import com.example.minitask_pacto_mais.config.N8nProperties;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EvolutionApiClient {

    private static final Logger log = LoggerFactory.getLogger(EvolutionApiClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private final EvolutionProperties properties;
    private final N8nProperties n8nProperties;
    private final RestClient.Builder restClientBuilder;

    public Map<String, Object> createInstance(String instanceName, boolean withQr) {
        ensureServer();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instanceName", instanceName);
        body.put("qrcode", withQr);
        body.put("integration", "WHATSAPP-BAILEYS");

        String webhookUrl = n8nProperties.resolveWebhookUrl();
        if (webhookUrl != null) {
            Map<String, Object> webhook = new LinkedHashMap<>();
            webhook.put("enabled", true);
            webhook.put("url", webhookUrl);
            webhook.put("byEvents", false);
            webhook.put("base64", false);
            webhook.put("events", List.of("MESSAGES_UPSERT"));
            body.put("webhook", webhook);
        }

        Map<String, Object> created = post("/instance/create", body);
        if (webhookUrl != null) {
            setMessagesUpsertWebhook(instanceName, webhookUrl);
        }
        return created;
    }

    public void setMessagesUpsertWebhook(String instanceName, String webhookUrl) {
        ensureServer();
        Map<String, Object> webhook = new LinkedHashMap<>();
        webhook.put("enabled", true);
        webhook.put("url", webhookUrl);
        webhook.put("webhookByEvents", false);
        webhook.put("byEvents", false);
        webhook.put("webhookBase64", false);
        webhook.put("base64", false);
        webhook.put("events", List.of("MESSAGES_UPSERT"));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("webhook", webhook);
        try {
            post("/webhook/set/" + encode(instanceName), body);
            log.info("Webhook n8n configurado na Evolution instance={} url={}", instanceName, webhookUrl);
        } catch (BusinessException ex) {
            log.warn("Falha ao setar webhook n8n instance={} erro={}", instanceName, ex.getMessage());
        }
    }

    public Map<String, Object> connect(String instanceName) {
        ensureServer();
        return get("/instance/connect/" + encode(instanceName));
    }

    public Map<String, Object> connectionState(String instanceName) {
        ensureServer();
        return get("/instance/connectionState/" + encode(instanceName));
    }

    public void sendText(String instanceName, String number, String text) {
        ensureServer();
        Map<String, Object> body = Map.of(
                "number", number,
                "text", text
        );
        post("/message/sendText/" + encode(instanceName), body);
    }

    public void logout(String instanceName) {
        ensureServer();
        try {
            delete("/instance/logout/" + encode(instanceName));
        } catch (BusinessException ignored) {
        }
    }

    private Map<String, Object> get(String path) {
        try {
            Map<String, Object> response = client()
                    .get()
                    .uri(url(path))
                    .retrieve()
                    .body(MAP_TYPE);
            return response != null ? response : Map.of();
        } catch (RestClientResponseException ex) {
            throw toBusiness(ex);
        }
    }

    private Map<String, Object> post(String path, Object body) {
        try {
            Map<String, Object> response = client()
                    .post()
                    .uri(url(path))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(MAP_TYPE);
            return response != null ? response : Map.of();
        } catch (RestClientResponseException ex) {
            throw toBusiness(ex);
        }
    }

    private void delete(String path) {
        try {
            client()
                    .delete()
                    .uri(url(path))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw toBusiness(ex);
        }
    }

    private RestClient client() {
        return restClientBuilder.build()
                .mutate()
                .defaultHeader("apikey", properties.apiKey())
                .build();
    }

    private String url(String path) {
        String base = trimTrailingSlash(properties.baseUrl());
        if (path.startsWith("/")) {
            return base + path;
        }
        return base + "/" + path;
    }

    private void ensureServer() {
        if (!properties.isServerConfigured()) {
            throw new BusinessException(
                    "Servidor WhatsApp não configurado no backend. Verifique o .env da API (URL e API key da Evolution).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private static BusinessException toBusiness(RestClientResponseException ex) {
        String detail = ex.getResponseBodyAsString();
        if (detail == null || detail.isBlank()) {
            detail = ex.getMessage();
        }
        return new BusinessException(
                "Erro Evolution (" + ex.getStatusCode().value() + "): " + detail,
                HttpStatus.BAD_GATEWAY);
    }

    private static String encode(String instanceName) {
        return UriComponentsBuilder.fromPath(instanceName).build().encode().toUriString();
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
