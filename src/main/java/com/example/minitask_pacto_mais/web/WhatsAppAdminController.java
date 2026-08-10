package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.notification.EvolutionProperties;
import com.example.minitask_pacto_mais.service.WhatsAppInstanceService;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.CreateInstanceRequest;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.EvolutionServerStatus;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.QrCodeResponse;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.WhatsAppInstanceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/whatsapp")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class WhatsAppAdminController {

    private final WhatsAppInstanceService whatsAppInstanceService;
    private final EvolutionProperties evolutionProperties;

    @GetMapping("/status")
    public EvolutionServerStatus status() {
        boolean ok = evolutionProperties.isServerConfigured();
        return new EvolutionServerStatus(
                ok,
                ok
                        ? "Evolution pronta no backend — só crie/conecte a instância."
                        : "Evolution sem URL/API key no .env do backend."
        );
    }

    @GetMapping("/instances")
    public List<WhatsAppInstanceResponse> list() {
        return whatsAppInstanceService.list();
    }

    @GetMapping("/instances/{id}")
    public WhatsAppInstanceResponse get(@PathVariable UUID id) {
        return whatsAppInstanceService.get(id);
    }

    @PostMapping("/instances")
    @ResponseStatus(HttpStatus.CREATED)
    public QrCodeResponse create(@Valid @RequestBody CreateInstanceRequest request) {
        return whatsAppInstanceService.create(request);
    }

    @GetMapping("/instances/{id}/qr")
    public QrCodeResponse qr(@PathVariable UUID id) {
        return whatsAppInstanceService.qr(id);
    }

    @PostMapping("/instances/{id}/refresh-status")
    public WhatsAppInstanceResponse refreshStatus(@PathVariable UUID id) {
        return whatsAppInstanceService.refreshStatus(id);
    }

    @PostMapping("/instances/{id}/activate")
    public WhatsAppInstanceResponse activate(@PathVariable UUID id) {
        return whatsAppInstanceService.activate(id);
    }

    @PostMapping("/instances/{id}/disconnect")
    public WhatsAppInstanceResponse disconnect(@PathVariable UUID id) {
        return whatsAppInstanceService.disconnect(id);
    }
}
