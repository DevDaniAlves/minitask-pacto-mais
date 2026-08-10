package com.example.minitask_pacto_mais.notification;

import com.example.minitask_pacto_mais.service.WhatsAppInstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EvolutionOtpSender {

    private final EvolutionProperties properties;
    private final EvolutionApiClient evolutionApiClient;
    private final WhatsAppInstanceService whatsAppInstanceService;

    public void sendWhatsApp(String destination, String message) {
        if (!properties.isServerConfigured()) {
            throw new IllegalStateException("Evolution API não configurada (URL/API key)");
        }

        String instance = whatsAppInstanceService.resolveActiveInstanceName()
                .orElse(properties.instance());
        if (instance == null || instance.isBlank()) {
            throw new IllegalStateException(
                    "Nenhuma instância WhatsApp ativa. Admin deve conectar em /api/admin/whatsapp");
        }

        String number = destination == null ? "" : destination.replaceAll("\\D", "");
        if (number.isBlank()) {
            throw new IllegalArgumentException("Telefone inválido para WhatsApp");
        }

        evolutionApiClient.sendText(instance, number, message);
    }
}
