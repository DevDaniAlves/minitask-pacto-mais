package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.service.ProfileService;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.MessageResponse;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.ChangePasswordRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.Disable2faRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.Enable2faRequest;
import com.example.minitask_pacto_mais.web.dtos.ProfileDtos.MeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<MeResponse> me() {
        return ResponseEntity.ok(profileService.me());
    }

    @PostMapping("/change-password")
    public ResponseEntity<MessageResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(profileService.changePassword(request));
    }

    @PostMapping("/2fa/enable")
    public ResponseEntity<MessageResponse> enable2fa(@Valid @RequestBody Enable2faRequest request) {
        return ResponseEntity.ok(profileService.enable2fa(request));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<MessageResponse> disable2fa(@Valid @RequestBody Disable2faRequest request) {
        return ResponseEntity.ok(profileService.disable2fa(request));
    }
}
