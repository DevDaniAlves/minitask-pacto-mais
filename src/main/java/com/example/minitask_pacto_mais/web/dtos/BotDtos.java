package com.example.minitask_pacto_mais.web.dtos;

import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class BotDtos {

    private BotDtos() {}

    public record MeResponse(
            UUID id,
            String name,
            String email,
            String phone,
            Role role
    ) {}

    public record IndexedBoard(
            int index,
            UUID id,
            String name,
            UUID teamId,
            String teamName
    ) {}

    public record IndexedUser(
            int index,
            UUID id,
            String name,
            String email
    ) {}

    public record TaskLookupItem(
            int index,
            UUID id,
            String title,
            TaskStatus status
    ) {}

    public record TaskLookupResponse(
            boolean multiple,
            TaskDetailResponse single,
            List<TaskLookupItem> matches,
            String hint
    ) {}

    public record TaskDetailResponse(
            UUID id,
            String title,
            String description,
            TaskStatus currentStatus,
            List<TaskStatus> allowedNextStatuses,
            Priority priority,
            UUID boardId,
            String boardName,
            UUID teamId,
            String teamName,
            UUID assigneeId,
            String assigneeName,
            LocalDateTime dueDate,
            Integer rating
    ) {}

    public record BotCreateTaskRequest(
            @NotBlank String title,
            LocalDateTime dueDate,
            UUID boardId,
            Integer boardIndex,
            UUID assigneeId,
            Integer assigneeIndex,
            Priority priority,
            String description
    ) {}
}
