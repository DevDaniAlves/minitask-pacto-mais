package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.service.UserService;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserRequest;
import com.example.minitask_pacto_mais.web.dtos.AuthDtos.AdminCreateUserResponse;
import com.example.minitask_pacto_mais.web.dtos.UserDtos.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCreateUserResponse create(@Valid @RequestBody AdminCreateUserRequest request) {
        return userService.createByAdmin(request);
    }

    @PostMapping("/{id}/resend-invite")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminCreateUserResponse resendInvite(@PathVariable UUID id) {
        return userService.resendInvite(id);
    }
}
