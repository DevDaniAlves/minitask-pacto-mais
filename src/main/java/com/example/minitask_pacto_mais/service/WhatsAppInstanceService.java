package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.domain.WhatsAppConnectionStatus;
import com.example.minitask_pacto_mais.domain.WhatsAppInstance;
import com.example.minitask_pacto_mais.notification.EvolutionApiClient;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.repository.WhatsAppInstanceRepository;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.CreateInstanceRequest;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.QrCodeResponse;
import com.example.minitask_pacto_mais.web.dtos.WhatsAppDtos.WhatsAppInstanceResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WhatsAppInstanceService {

    private final WhatsAppInstanceRepository repository;
    private final UserRepository userRepository;
    private final EvolutionApiClient evolutionApiClient;

    @Transactional(readOnly = true)
    public List<WhatsAppInstanceResponse> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WhatsAppInstanceResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Transactional
    public QrCodeResponse create(CreateInstanceRequest request) {
        String name = request.instanceName().trim();
        if (repository.existsByInstanceNameIgnoreCase(name)) {
            throw new BusinessException("Já existe uma instância com esse nome", HttpStatus.CONFLICT);
        }

        User admin = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Map<String, Object> created = evolutionApiClient.createInstance(name, true);

        WhatsAppInstance entity = WhatsAppInstance.builder()
                .instanceName(name)
                .status(WhatsAppConnectionStatus.CONNECTING)
                .active(false)
                .createdBy(admin)
                .build();
        repository.save(entity);

        QrCodeResponse qr = extractQr(entity, created);
        if (qr.qrBase64() == null && qr.code() == null && qr.pairingCode() == null) {
            Map<String, Object> connect = evolutionApiClient.connect(name);
            return extractQr(entity, connect);
        }
        return qr;
    }

    @Transactional(readOnly = true)
    public QrCodeResponse qr(UUID id) {
        WhatsAppInstance entity = find(id);
        Map<String, Object> connect = evolutionApiClient.connect(entity.getInstanceName());
        return extractQr(entity, connect);
    }

    @Transactional
    public WhatsAppInstanceResponse refreshStatus(UUID id) {
        WhatsAppInstance entity = find(id);
        Map<String, Object> state = evolutionApiClient.connectionState(entity.getInstanceName());
        String connection = extractConnectionState(state);

        if ("open".equalsIgnoreCase(connection)) {
            entity.setStatus(WhatsAppConnectionStatus.CONNECTED);
            if (entity.getConnectedAt() == null) {
                entity.setConnectedAt(LocalDateTime.now());
            }
            String phone = extractOwnerPhone(state);
            if (phone != null) {
                entity.setOwnerPhone(phone);
            }
            if (!entity.isActive()) {
                repository.clearActiveFlags();
                entity.setActive(true);
            }
        } else if ("connecting".equalsIgnoreCase(connection)) {
            entity.setStatus(WhatsAppConnectionStatus.CONNECTING);
            entity.setActive(false);
        } else if ("close".equalsIgnoreCase(connection) || "closed".equalsIgnoreCase(connection)) {
            entity.setStatus(WhatsAppConnectionStatus.DISCONNECTED);
            entity.setActive(false);
        } else if (connection != null && !connection.isBlank()) {
            entity.setStatus(WhatsAppConnectionStatus.PENDING);
        }

        return toResponse(entity);
    }

    @Transactional
    public WhatsAppInstanceResponse activate(UUID id) {
        WhatsAppInstance entity = find(id);
        if (entity.getStatus() != WhatsAppConnectionStatus.CONNECTED) {
            throw new BusinessException(
                    "Só é possível ativar uma instância CONNECTED. Atualize o status após escanear o QR.",
                    HttpStatus.BAD_REQUEST);
        }
        repository.clearActiveFlags();
        entity.setActive(true);
        return toResponse(entity);
    }

    @Transactional
    public WhatsAppInstanceResponse disconnect(UUID id) {
        WhatsAppInstance entity = find(id);
        try {
            evolutionApiClient.logout(entity.getInstanceName());
        } catch (BusinessException ex) {
        }
        entity.setStatus(WhatsAppConnectionStatus.DISCONNECTED);
        entity.setActive(false);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public Optional<String> resolveActiveInstanceName() {
        return repository.findFirstByActiveTrueAndStatus(WhatsAppConnectionStatus.CONNECTED)
                .map(WhatsAppInstance::getInstanceName);
    }

    private WhatsAppInstance find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new BusinessException("Instância WhatsApp não encontrada", HttpStatus.NOT_FOUND));
    }

    private WhatsAppInstanceResponse toResponse(WhatsAppInstance entity) {
        return new WhatsAppInstanceResponse(
                entity.getId(),
                entity.getInstanceName(),
                entity.getStatus(),
                entity.getOwnerPhone(),
                entity.isActive(),
                entity.getConnectedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    @SuppressWarnings("unchecked")
    private QrCodeResponse extractQr(WhatsAppInstance entity, Map<String, Object> payload) {
        String pairingCode = asString(payload.get("pairingCode"));
        String code = asString(payload.get("code"));
        String base64 = asString(payload.get("base64"));

        Object qrcode = payload.get("qrcode");
        if (qrcode instanceof Map<?, ?> qrMap) {
            if (base64 == null) {
                base64 = asString(qrMap.get("base64"));
            }
            if (code == null) {
                code = asString(qrMap.get("code"));
            }
        }

        Object data = payload.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            if (base64 == null) {
                base64 = asString(dataMap.get("base64"));
            }
            if (pairingCode == null) {
                pairingCode = asString(dataMap.get("pairingCode"));
            }
        }

        return new QrCodeResponse(
                entity.getId(),
                entity.getInstanceName(),
                entity.getStatus(),
                pairingCode,
                base64,
                code);
    }

    @SuppressWarnings("unchecked")
    private String extractConnectionState(Map<String, Object> state) {
        Object instance = state.get("instance");
        if (instance instanceof Map<?, ?> map) {
            Object s = map.get("state");
            if (s == null) {
                s = map.get("status");
            }
            if (s != null) {
                return String.valueOf(s);
            }
        }
        Object direct = state.get("state");
        if (direct != null) {
            return String.valueOf(direct);
        }
        Object status = state.get("status");
        return status != null ? String.valueOf(status) : null;
    }

    private String extractOwnerPhone(Map<String, Object> state) {
        Object instance = state.get("instance");
        if (instance instanceof Map<?, ?> map) {
            Object owner = map.get("owner");
            if (owner != null) {
                return String.valueOf(owner).replaceAll("\\D", "");
            }
            Object number = map.get("number");
            if (number != null) {
                return String.valueOf(number).replaceAll("\\D", "");
            }
        }
        return null;
    }

    private static String asString(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value);
        return s.isBlank() ? null : s;
    }
}
