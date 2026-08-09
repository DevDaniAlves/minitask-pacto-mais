package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.service.AuthService;
import com.example.minitask_pacto_mais.service.PasswordResetService;
import com.example.minitask_pacto_mais.service.TwoFactorService;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AuthResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.ForgotPasswordRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.LoginRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.MessageResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.RegisterRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.ResetPasswordRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.Verify2faRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.VerifyPhoneRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final TwoFactorService twoFactorService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/verify-phone")
    public ResponseEntity<AuthResponse> verifyPhone(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody VerifyPhoneRequest request
    ) {
        UUID userId = authService.requireUserIdFromBearer(authorization);
        return ResponseEntity.ok(authService.verifyPhone(userId, request.code()));
    }

    @PostMapping("/resend-phone-otp")
    public ResponseEntity<AuthResponse> resendPhoneOtp(
            @RequestHeader("Authorization") String authorization
    ) {
        UUID userId = authService.requireUserIdFromBearer(authorization);
        return ResponseEntity.ok(authService.resendPhoneOtp(userId));
    }

    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verify2fa(@Valid @RequestBody Verify2faRequest request) {
        return ResponseEntity.ok(twoFactorService.verify2fa(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<OtpChannel> channel = passwordResetService.forgotPassword(request);
        String channelName = channel.map(Enum::name).orElse("NONE");
        return ResponseEntity.ok(new MessageResponse(
                "Se o e-mail existir, enviamos um código de recuperação",
                channelName
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Senha alterada com sucesso", null));
    }
}
