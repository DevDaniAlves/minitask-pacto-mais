package com.example.minitask_pacto_mais.web.dtos;

import com.example.minitask_pacto_mais.domain.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class ProfileDtos {

    private ProfileDtos() {}

    public record MeResponse(
            UUID id,
            String name,
            String email,
            String phone,
            boolean phoneVerified,
            Role role,
            boolean twoFactorEnabled,
            boolean twoFactorPending
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}

    public record Enable2faRequest(
            String phone
    ) {}

    public record Disable2faRequest(
            @NotBlank String password
    ) {}
}
