package com.example.minitask_pacto_mais.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.n8n")
public record N8nProperties(String webhookUrl, String baseUrl) {

    private static final String DEFAULT_PATH = "/webhook/minitask-whatsapp";

    public String resolveWebhookUrl() {
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            return trimTrailingSlash(webhookUrl.trim());
        }
        if (baseUrl != null && !baseUrl.isBlank()) {
            return trimTrailingSlash(baseUrl.trim()) + DEFAULT_PATH;
        }
        return null;
    }

    public boolean hasWebhook() {
        return resolveWebhookUrl() != null;
    }

    private static String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
