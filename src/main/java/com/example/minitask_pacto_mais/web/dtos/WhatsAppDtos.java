package com.example.minitask_pacto_mais.web.dtos;

import com.example.minitask_pacto_mais.domain.WhatsAppConnectionStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class WhatsAppDtos {

    private WhatsAppDtos() {}

    public record CreateInstanceRequest(
            @NotBlank @Size(min = 2, max = 100) String instanceName
    ) {}

    public record WhatsAppInstanceResponse(
            UUID id,
            String instanceName,
            WhatsAppConnectionStatus status,
            String ownerPhone,
            boolean active,
            LocalDateTime connectedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}

    public record QrCodeResponse(
            UUID id,
            String instanceName,
            WhatsAppConnectionStatus status,
            String pairingCode,
            String qrBase64,
            String code
    ) {}

    public record EvolutionServerStatus(boolean configured, String message) {}
}
