package com.example.minitask_pacto_mais.web.dtos;
import com.example.minitask_pacto_mais.domain.Role;
import java.util.UUID;
public final class UserDtos {
    private UserDtos() {}
    public record UserResponse(
            UUID id,
            String name,
            String email,
            Role role
    ) {}
}