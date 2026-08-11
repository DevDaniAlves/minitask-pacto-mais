package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.JwtService;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AuthResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.RegisterRequest;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private OtpService otpService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerCriaUsuarioComSenhaHasheada() {
        when(userRepository.existsByEmailIgnoreCase("ana@demo.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("hash");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        AuthResponse response = authService.register(new RegisterRequest(
                "Ana",
                "ana@demo.com",
                "senha123",
                Role.FUNCIONARIO,
                false,
                null
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertEquals("Ana", saved.getName());
        assertEquals("ana@demo.com", saved.getEmail());
        assertEquals("hash", saved.getPasswordHash());
        assertEquals(Role.FUNCIONARIO, saved.getRole());
        assertFalse(saved.isTwoFactorEnabled());
        assertEquals("jwt-token", response.token());
        verify(otpService, never()).issuePhoneVerificationOtp(any());
    }

    @Test
    void registerFalhaSeEmailJaExiste() {
        when(userRepository.existsByEmailIgnoreCase("ana@demo.com")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(new RegisterRequest(
                        "Ana",
                        "ana@demo.com",
                        "senha123",
                        Role.FUNCIONARIO,
                        false,
                        null
                )));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerCom2faSemTelefoneFalha() {
        when(userRepository.existsByEmailIgnoreCase("ana@demo.com")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.register(new RegisterRequest(
                        "Ana",
                        "ana@demo.com",
                        "senha123",
                        Role.FUNCIONARIO,
                        true,
                        null
                )));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(userRepository, never()).save(any());
    }
}
