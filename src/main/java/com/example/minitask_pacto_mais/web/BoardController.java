package com.example.minitask_pacto_mais.web;

import com.example.minitask_pacto_mais.service.BoardService;
import com.example.minitask_pacto_mais.service.TaskService;
import com.example.minitask_pacto_mais.web.dtos.BoardDtos.BoardRequest;
import com.example.minitask_pacto_mais.web.dtos.BoardDtos.BoardResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.KanbanResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/api/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;
    private final TaskService taskService;

    @GetMapping
    public List<BoardResponse> list(@RequestParam(required = false) UUID teamId) {
        return boardService.list(teamId);
    }

    @GetMapping("/{id}")
    public BoardResponse get(@PathVariable UUID id) {
        return boardService.get(id);
    }

    @GetMapping("/{id}/kanban")
    public KanbanResponse kanban(@PathVariable UUID id) {
        return taskService.kanbanByBoard(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public BoardResponse create(@Valid @RequestBody BoardRequest request) {
        return boardService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BoardResponse update(@PathVariable UUID id, @Valid @RequestBody BoardRequest request) {
        return boardService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        boardService.delete(id);
    }
}
