package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.util.PhoneNormalizer;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.MessageResponse;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.ChangePasswordRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.Disable2faRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.Enable2faRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.MeResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final String DEFAULT_COUNTRY = "55";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;

    @Transactional(readOnly = true)
    public MeResponse me() {
        return toMe(loadCurrentUser());
    }

    @Transactional
    public MessageResponse changePassword(ChangePasswordRequest request) {
        User user = loadCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Senha atual incorreta", HttpStatus.UNAUTHORIZED);
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("A nova senha deve ser diferente da atual", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        return new MessageResponse("Senha alterada com sucesso", null);
    }

    @Transactional
    public MessageResponse enable2fa(Enable2faRequest request) {
        User user = loadCurrentUser();
        if (user.isTwoFactorEnabled()) {
            throw new BusinessException("2FA já está ativo", HttpStatus.BAD_REQUEST);
        }

        String phone = PhoneNormalizer.normalize(request.phone(), DEFAULT_COUNTRY);
        if (phone == null) {
            phone = user.getPhone();
        }
        if (phone == null) {
            throw new BusinessException(
                    "Telefone é obrigatório para ativar a verificação em duas etapas",
                    HttpStatus.BAD_REQUEST);
        }

        ensurePhoneAvailable(phone, user.getId());

        boolean phoneChanged = !Objects.equals(phone, user.getPhone());
        user.setPhone(phone);
        if (phoneChanged) {
            user.setPhoneVerified(false);
        }

        user.setTwoFactorPending(true);
        user.setTwoFactorEnabled(false);

        OtpChannel channel = otpService.issuePhoneVerificationOtp(user);
        return new MessageResponse(
                "Enviamos um código para confirmar a ativação do 2FA. Use POST /api/auth/verify-phone",
                channel.name());
    }

    @Transactional
    public MessageResponse disable2fa(Disable2faRequest request) {
        User user = loadCurrentUser();
        if (!user.isTwoFactorEnabled() && !user.isTwoFactorPending()) {
            throw new BusinessException("2FA não está ativo", HttpStatus.BAD_REQUEST);
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Senha incorreta", HttpStatus.UNAUTHORIZED);
        }
        user.setTwoFactorEnabled(false);
        user.setTwoFactorPending(false);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
        return new MessageResponse("2FA desativado com sucesso", null);
    }

    private User loadCurrentUser() {
        UUID id = SecurityUtils.currentUser().getId();
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    private void ensurePhoneAvailable(String phone, UUID currentUserId) {
        userRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(currentUserId)) {
                throw new BusinessException("Telefone já está em uso", HttpStatus.CONFLICT);
            }
        });
    }

    private MeResponse toMe(User user) {
        return new MeResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isPhoneVerified(),
                user.getRole(),
                user.isTwoFactorEnabled(),
                user.isTwoFactorPending());
    }
}
