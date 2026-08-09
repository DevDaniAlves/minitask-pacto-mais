package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.ForgotPasswordRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.springframework.http.HttpStatus;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Optional<OtpChannel> forgotPassword(ForgotPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
        if (userOpt.isEmpty()) {
            log.info("Forgot password para e-mail inexistente: {}", email);
            return Optional.empty();
        }
        OtpChannel channel = otpService.issuePasswordResetOtp(userOpt.get());
        return Optional.of(channel);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BusinessException("Código inválido", HttpStatus.UNAUTHORIZED));

        otpService.verifyResetOtp(user, request.code());
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
