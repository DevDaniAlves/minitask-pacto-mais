package com.example.minitask_pacto_mais.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.JwtService;
import com.example.minitask_pacto_mais.util.PhoneNormalizer;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AuthResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.LoginChallengeResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.LoginRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.RegisterRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.Verify2faRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.springframework.http.HttpStatus;


import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String DEFAULT_COUNTRY = "55";
    private static final long TEMP_TOKEN_TTL_MS = 10 * 60 * 1000L;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = PhoneNormalizer.normalize(request.phone(), DEFAULT_COUNTRY);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email já está em uso", HttpStatus.CONFLICT);
        }

        if (request.enableTwoFactor() && phone == null) {
            throw new BusinessException("Telefone é obrigatório para ativar a verificação em duas etapas", HttpStatus.BAD_REQUEST);
        }

        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException("Telefone já está em uso", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .phone(phone)
                .phoneVerified(false)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .twoFactorEnabled(false)
                .twoFactorPending(request.enableTwoFactor())
                .build();

        userRepository.save(user);

        boolean needsPhoneVerification = request.enableTwoFactor()
                || (phone != null && !user.isPhoneVerified());

        if (needsPhoneVerification && phone != null) {
            otpService.issuePhoneVerificationOtp(user);
        }

        return toResponse(user, needsPhoneVerification);
    }

    @Transactional
    public Object login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Email ou senha inválidos", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Email ou senha inválidos", HttpStatus.UNAUTHORIZED);
        }

        if (user.isTwoFactorEnabled()) {
            OtpChannel channel = otpService.issueLoginTwoFactorOtp(user);
            String tempToken = jwtService.generateTempToken(user, "2FA_PENDING", TEMP_TOKEN_TTL_MS);
            return new LoginChallengeResponse(true, tempToken, channel.name());
        }

        boolean needsPhoneVerification = user.isTwoFactorPending()
                || (user.getPhone() != null && !user.isPhoneVerified());

        return toResponse(user, needsPhoneVerification);
    }

    @Transactional
    public AuthResponse verifyPhone(UUID userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        otpService.verifyPhoneOtp(user, code);
        return toResponse(user, false);
    }

    @Transactional
    public AuthResponse resendPhoneOtp(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        if (user.getPhone() == null) {
            throw new BusinessException("Usuário não possui telefone", HttpStatus.BAD_REQUEST);
        }
        if (user.isPhoneVerified() && !user.isTwoFactorPending()) {
            throw new BusinessException("Telefone já verificado", HttpStatus.BAD_REQUEST);
        }
        otpService.issuePhoneVerificationOtp(user);
        return toResponse(user, true);
    }

    @Transactional
    public AuthResponse verify2fa(Verify2faRequest request) {
        DecodedJWT jwt = jwtService.verify(request.tempToken());
        String purpose = jwt.getClaim("purpose").asString();
        if (!"2FA_PENDING".equals(purpose)) {
            throw new BusinessException("Token temporário inválido", HttpStatus.BAD_REQUEST);
        }

        UUID userId = jwtService.extractUserId(jwt);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        otpService.verifyLoginOtp(user, request.code());
        return toResponse(user, false);
    }

    public UUID requireUserIdFromBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException("Token ausente", HttpStatus.UNAUTHORIZED);
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        DecodedJWT jwt = jwtService.verify(token);
        return jwtService.extractUserId(jwt);
    }

    private AuthResponse toResponse(User user, boolean needsPhoneVerification) {
        return new AuthResponse(
                jwtService.generateToken(user),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getRole(),
                user.isTwoFactorEnabled(),
                needsPhoneVerification
        );
    }
}
