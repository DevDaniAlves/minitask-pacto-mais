package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.service.CommentService;
import com.example.minitask_pacto_mais.service.TaskService;
import com.example.minitask_pacto_mais.web.dtos.CommentDtos.CommentRequest;
import com.example.minitask_pacto_mais.web.dtos.CommentDtos.CommentResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.EvaluationRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.KanbanResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.PageResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.StatusUpdateRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final CommentService commentService;

    @GetMapping
    public PageResponse<TaskResponse> search(
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) UUID boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return taskService.search(status, priority, assigneeId, teamId, boardId, page, size);
    }

    @GetMapping("/kanban")
    public List<KanbanResponse> kanbanOverview(@RequestParam(required = false) UUID teamId) {
        return taskService.kanbanOverview(teamId);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@PathVariable UUID id) {
        return taskService.get(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@Valid @RequestBody TaskRequest request) {
        return taskService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public TaskResponse update(@PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        taskService.delete(id);
    }

    @PatchMapping("/{id}/status")
    public TaskResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return taskService.changeStatus(id, request);
    }

    @PostMapping("/{id}/evaluation")
    @PreAuthorize("hasRole('ADMIN')")
    public TaskResponse evaluate(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return taskService.evaluate(id, request);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> listComments(@PathVariable UUID id) {
        return commentService.listByTask(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable UUID id,
            @Valid @RequestBody CommentRequest request
    ) {
        return commentService.create(id, request);
    }
}
