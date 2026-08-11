package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.OtpChannel;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserResponse;
import com.example.minitask_pacto_mais.web.dtos.UserDtos.UserResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OtpService otpService;
    @Mock
    private TeamService teamService;

    @InjectMocks
    private UserService userService;

    @Test
    void createByAdminPersisteUsuarioEEnviaConvite() {
        when(userRepository.existsByEmailIgnoreCase("func@demo.com")).thenReturn(false);
        when(userRepository.existsByPhone("+5511999999999")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(otpService.issueAccountSetupOtp(any(User.class))).thenReturn(OtpChannel.WHATSAPP);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AdminCreateUserResponse response = userService.createByAdmin(new AdminCreateUserRequest(
                "Func",
                "func@demo.com",
                "11999999999",
                Role.FUNCIONARIO,
                null
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("Func", saved.getName());
        assertEquals("+5511999999999", saved.getPhone());
        assertFalse(saved.isPhoneVerified());
        assertFalse(saved.isTwoFactorEnabled());
        assertEquals(true, saved.isMustChangePassword());
        assertEquals("func@demo.com", response.email());
        assertEquals("WHATSAPP", response.deliveryChannel());
        verify(otpService).issueAccountSetupOtp(any(User.class));
    }

    @Test
    void createByAdminFalhaSemTelefone() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.createByAdmin(new AdminCreateUserRequest(
                        "Func",
                        "func@demo.com",
                        "abc",
                        Role.FUNCIONARIO,
                        null
                )));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void updatePhoneRemoveTelefoneEDesliga2fa() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .name("Func")
                .email("func@demo.com")
                .phone("+5511999999999")
                .phoneVerified(true)
                .role(Role.FUNCIONARIO)
                .twoFactorEnabled(true)
                .twoFactorPending(false)
                .otpHash("x")
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        UserResponse response = userService.updatePhone(id, null);

        assertNull(user.getPhone());
        assertFalse(user.isPhoneVerified());
        assertFalse(user.isTwoFactorEnabled());
        assertFalse(user.isTwoFactorPending());
        assertNull(user.getOtpHash());
        assertNull(response.phone());
        assertFalse(response.twoFactorEnabled());
    }

    @Test
    void updatePhoneAdicionaTelefone() {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .name("Func")
                .email("func@demo.com")
                .phone(null)
                .role(Role.FUNCIONARIO)
                .twoFactorEnabled(false)
                .build();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userRepository.findByPhone("+5511888777666")).thenReturn(Optional.empty());

        UserResponse response = userService.updatePhone(id, "11888777666");

        assertEquals("+5511888777666", user.getPhone());
        assertFalse(user.isPhoneVerified());
        assertEquals("+5511888777666", response.phone());
    }
}
