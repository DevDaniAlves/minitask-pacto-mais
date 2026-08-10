package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.config.FrontendProperties;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.notification.OtpSender;
import com.example.minitask_pacto_mais.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int CODE_LENGTH = 6;
    private static final int TTL_MINUTES = 10;

    private final UserRepository userRepository;
    private final OtpSender otpSender;
    private final FrontendProperties frontendProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public OtpChannel issuePhoneVerificationOtp(User user) {
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            throw new BusinessException("Usuário não possui telefone cadastrado", HttpStatus.BAD_REQUEST);
        }
        String code = generateCode();
        user.setOtpHash(hash(code));
        user.setOtpExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        userRepository.save(user);

        otpSender.send(
                OtpChannel.WHATSAPP,
                user.getPhone(),
                "Seu código Mini Task: " + code + " (válido por " + TTL_MINUTES + " min)",
                user.getEmail()
        );
        return OtpChannel.WHATSAPP;
    }

    @Transactional
    public OtpChannel issuePasswordResetOtp(User user) {
        String code = generateCode();
        user.setResetPasswordTokenHash(hash(code));
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        userRepository.save(user);

        String resetMessage = "Código para redefinir senha: " + code + " (válido por " + TTL_MINUTES + " min)";

        if (user.getPhone() != null && user.isPhoneVerified()) {
            otpSender.send(
                    OtpChannel.WHATSAPP,
                    user.getPhone(),
                    resetMessage,
                    user.getEmail()
            );
            return OtpChannel.WHATSAPP;
        }

        otpSender.send(
                OtpChannel.EMAIL,
                user.getEmail(),
                resetMessage,
                null
        );
        return OtpChannel.EMAIL;
    }

    @Transactional
    public OtpChannel issueAccountSetupOtp(User user) {
        String code = generateCode();
        user.setResetPasswordTokenHash(hash(code));
        user.setResetPasswordTokenExpiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES));
        user.setMustChangePassword(true);
        userRepository.save(user);

        String link = frontendProperties.baseUrlOrDefault()
                + "/set-password?email="
                + java.net.URLEncoder.encode(user.getEmail(), java.nio.charset.StandardCharsets.UTF_8)
                + "&code=" + code;
        String message = "Bem-vindo ao Mini Task! Abra o link para definir sua senha: "
                + link
                + " (válido por " + TTL_MINUTES + " min).";

        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            try {
                otpSender.send(OtpChannel.WHATSAPP, user.getPhone(), message, user.getEmail());
                return OtpChannel.WHATSAPP;
            } catch (Exception ex) {
                otpSender.send(OtpChannel.EMAIL, user.getEmail(), message, null);
                return OtpChannel.EMAIL;
            }
        }

        otpSender.send(OtpChannel.EMAIL, user.getEmail(), message, null);
        return OtpChannel.EMAIL;
    }

    @Transactional
    public OtpChannel issueLoginTwoFactorOtp(User user) {
        if (!user.isTwoFactorEnabled()) {
            throw new BusinessException("2FA não está ativo para este usuário", HttpStatus.BAD_REQUEST);
        }
        if (user.getPhone() == null || !user.isPhoneVerified()) {
            throw new BusinessException("Telefone verificado é obrigatório para 2FA", HttpStatus.BAD_REQUEST);
        }
        return issuePhoneVerificationOtp(user);
    }

    @Transactional
    public void verifyPhoneOtp(User user, String rawCode) {
        assertValidOtp(user.getOtpHash(), user.getOtpExpiresAt(), rawCode);
        user.setPhoneVerified(true);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);

        if (user.isTwoFactorPending()) {
            user.setTwoFactorEnabled(true);
            user.setTwoFactorPending(false);
        }
        userRepository.save(user);
    }

    @Transactional
    public void verifyLoginOtp(User user, String rawCode) {
        assertValidOtp(user.getOtpHash(), user.getOtpExpiresAt(), rawCode);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void verifyResetOtp(User user, String rawCode) {
        assertValidOtp(user.getResetPasswordTokenHash(), user.getResetPasswordTokenExpiresAt(), rawCode);
        user.setResetPasswordTokenHash(null);
        user.setResetPasswordTokenExpiresAt(null);
        userRepository.save(user);
    }

    public String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int n = secureRandom.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", n);
    }

    public String hash(String rawCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawCode.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
    }

    private void assertValidOtp(String storedHash, LocalDateTime expiresAt, String rawCode) {
        if (storedHash == null || expiresAt == null) {
            throw new BusinessException("Nenhum código pendente", HttpStatus.BAD_REQUEST);
        }
        if (LocalDateTime.now().isAfter(expiresAt)) {
            throw new BusinessException("Código expirado", HttpStatus.BAD_REQUEST);
        }
        if (rawCode == null || rawCode.isBlank() || !storedHash.equals(hash(rawCode))) {
            throw new BusinessException("Código inválido", HttpStatus.UNAUTHORIZED);
        }
    }
}
