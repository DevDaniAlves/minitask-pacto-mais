package com.example.minitask_pacto_mais.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class TeamDtos {

    private TeamDtos() {}

    public record TeamRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 32) String color
    ) {}

    public record MemberRequest(
            @NotNull UUID userId
    ) {}

    public record MemberResponse(
            UUID userId,
            String name,
            String email,
            boolean enabled
    ) {}

    public record TeamResponse(
            UUID id,
            String name,
            String color,
            List<MemberResponse> members
    ) {}
}
