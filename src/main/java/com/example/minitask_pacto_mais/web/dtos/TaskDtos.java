package com.example.minitask_pacto_mais.web.dtos;

import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TaskDtos {

    private TaskDtos() {}

    public record TaskRequest(
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull Priority priority,
            @NotNull UUID boardId,
            UUID assigneeId,
            LocalDateTime dueDate
    ) {}

    public record TaskResponse(
            UUID id,
            String title,
            String description,
            TaskStatus status,
            Priority priority,
            UUID boardId,
            String boardName,
            UUID teamId,
            String teamName,
            String teamColor,
            UUID assigneeId,
            String assigneeName,
            UUID createdById,
            String createdByName,
            LocalDateTime createdAt,
            LocalDateTime dueDate,
            LocalDateTime updatedAt,
            Integer rating,
            String ratingDescription
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record StatusUpdateRequest(
            @NotNull TaskStatus status
    ) {}

    public record EvaluationRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @NotNull EvaluationOutcome outcome,
            String comment
    ) {}

    public enum EvaluationOutcome {
        APPROVED,
        REJECTED
    }

    public record KanbanResponse(
            UUID boardId,
            String boardName,
            UUID teamId,
            String teamName,
            String teamColor,
            Map<TaskStatus, List<TaskResponse>> columns
    ) {}
}
