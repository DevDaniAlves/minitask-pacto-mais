package com.example.minitask_pacto_mais.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class BoardDtos {

    private BoardDtos() {}

    public record BoardRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull UUID teamId
    ) {}

    public record BoardResponse(
            UUID id,
            String name,
            UUID teamId,
            String teamName,
            String teamColor
    ) {}
}
