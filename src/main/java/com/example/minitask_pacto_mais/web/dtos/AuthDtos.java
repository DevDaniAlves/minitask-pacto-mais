package com.example.minitask_pacto_mais.web.dtos;

import com.example.minitask_pacto_mais.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class AuthDtos {

    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, max = 100) String password,
            @NotNull Role role,
            boolean enableTwoFactor,
            String phone
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record AuthResponse(
            String token,
            UUID userId,
            String name,
            String email,
            String phone,
            boolean phoneVerified,
            Role role,
            boolean twoFactorEnabled,
            boolean needsPhoneVerification
    ) {}

    public record LoginChallengeResponse(
            boolean requires2fa,
            String tempToken,
            String deliveryChannel
    ) {}

    public record VerifyPhoneRequest(
            @NotBlank String code
    ) {}

    public record Verify2faRequest(
            @NotBlank String tempToken,
            @NotBlank String code
    ) {}

    public record ForgotPasswordRequest(
            @NotBlank @Email String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank @Email String email,
            @NotBlank String code,
            @NotBlank @Size(min = 6, max = 100) String newPassword
    ) {}

    public record MessageResponse(String message, String deliveryChannel) {}
}
