package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.service.BotService;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.BotCreateTaskRequest;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.IndexedBoard;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.IndexedUser;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.MeResponse;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.TaskDetailResponse;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.TaskLookupResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.EvaluationRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.StatusUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot")
@RequiredArgsConstructor
public class BotController {

    private final BotService botService;

    @GetMapping("/me")
    public MeResponse me() {
        return botService.me();
    }

    @GetMapping("/boards")
    public List<IndexedBoard> boards() {
        return botService.listBoards();
    }

    @GetMapping("/users")
    public List<IndexedUser> users(@RequestParam UUID boardId) {
        return botService.listUsersForBoard(boardId);
    }

    @GetMapping("/tasks/lookup")
    public TaskLookupResponse lookup(@RequestParam String q) {
        return botService.lookup(q);
    }

    @GetMapping("/tasks/{id}")
    public TaskDetailResponse getTask(@PathVariable UUID id) {
        return botService.getTask(id);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskDetailResponse changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return botService.changeStatus(id, request);
    }

    @PostMapping("/tasks/{id}/evaluate")
    public TaskDetailResponse evaluate(
            @PathVariable UUID id,
            @Valid @RequestBody EvaluationRequest request
    ) {
        return botService.evaluate(id, request);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDetailResponse create(@Valid @RequestBody BotCreateTaskRequest request) {
        return botService.createTask(request);
    }
}
