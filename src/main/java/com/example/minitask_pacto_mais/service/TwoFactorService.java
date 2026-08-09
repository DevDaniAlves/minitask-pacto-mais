package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AuthResponse;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.Verify2faRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final AuthService authService;

    public AuthResponse verify2fa(Verify2faRequest request) {
        return authService.verify2fa(request);
    }
}
