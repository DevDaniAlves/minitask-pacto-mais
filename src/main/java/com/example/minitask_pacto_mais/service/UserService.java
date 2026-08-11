package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.util.PhoneNormalizer;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserResponse;
import com.example.minitask_pacto_mais.web.dtos.UserDtos.UserResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_COUNTRY = "55";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final TeamService teamService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updatePhone(UUID userId, String rawPhone) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        boolean clearing = rawPhone == null || rawPhone.isBlank();
        if (clearing) {
            user.setPhone(null);
            user.setPhoneVerified(false);
            clearTwoFactor(user);
            return toResponse(user);
        }

        String phone = PhoneNormalizer.normalize(rawPhone, DEFAULT_COUNTRY);
        if (phone == null) {
            throw new BusinessException("Telefone inválido", HttpStatus.BAD_REQUEST);
        }

        userRepository.findByPhone(phone).ifPresent(existing -> {
            if (!existing.getId().equals(userId)) {
                throw new BusinessException("Telefone já está em uso", HttpStatus.CONFLICT);
            }
        });

        boolean phoneChanged = !phone.equals(user.getPhone());
        user.setPhone(phone);
        if (phoneChanged) {
            user.setPhoneVerified(false);
            clearTwoFactor(user);
        }
        return toResponse(user);
    }

    private void clearTwoFactor(User user) {
        user.setTwoFactorEnabled(false);
        user.setTwoFactorPending(false);
        user.setOtpHash(null);
        user.setOtpExpiresAt(null);
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole(),
                u.isMustChangePassword(),
                u.isPhoneVerified(),
                u.isTwoFactorEnabled(),
                u.isTwoFactorPending()
        );
    }

    @Transactional
    public AdminCreateUserResponse createByAdmin(AdminCreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        String phone = PhoneNormalizer.normalize(request.phone(), DEFAULT_COUNTRY);
        if (phone == null) {
            throw new BusinessException("Telefone é obrigatório para convidar o funcionário", HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email já está em uso", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByPhone(phone)) {
            throw new BusinessException("Telefone já está em uso", HttpStatus.CONFLICT);
        }

        Role role = request.role() != null ? request.role() : Role.FUNCIONARIO;
        String randomSecret = randomPassword();

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .phone(phone)
                .phoneVerified(false)
                .passwordHash(passwordEncoder.encode(randomSecret))
                .role(role)
                .mustChangePassword(true)
                .twoFactorEnabled(false)
                .twoFactorPending(false)
                .build();
        userRepository.save(user);

        if (request.teamId() != null) {
            teamService.addMember(request.teamId(), user.getId());
        }

        OtpChannel channel = otpService.issueAccountSetupOtp(user);
        return new AdminCreateUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                request.teamId() != null
                        ? "Convite enviado e usuário adicionado ao time. Defina a senha em /set-password."
                        : "Convite enviado. O usuário deve definir a senha em /set-password com o código recebido.",
                channel.name()
        );
    }

    @Transactional
    public AdminCreateUserResponse resendInvite(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        if (user.getPhone() == null) {
            throw new BusinessException("Usuário sem telefone cadastrado", HttpStatus.BAD_REQUEST);
        }
        OtpChannel channel = otpService.issueAccountSetupOtp(user);
        return new AdminCreateUserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                "Novo código de convite enviado",
                channel.name()
        );
    }

    private String randomPassword() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }
}
