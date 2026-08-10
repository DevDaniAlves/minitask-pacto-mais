package com.example.minitask_pacto_mais.web.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public final class CommentDtos {

    private CommentDtos() {}

    public record CommentRequest(
            @NotBlank @Size(max = 5000) String body
    ) {}

    public record CommentResponse(
            UUID id,
            UUID taskId,
            UUID authorId,
            String authorName,
            String body,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {}
}