package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Board;
import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.domain.TaskStatusLog;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.WhatsAppNotifier;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TaskRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.EvaluationOutcome;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.EvaluationRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.KanbanResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.PageResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.StatusUpdateRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;
    private final WhatsAppNotifier whatsAppNotifier;

    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> search(
            TaskStatus status,
            Priority priority,
            UUID assigneeId,
            UUID teamId,
            UUID boardId,
            int page,
            int size) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        Page<Task> result = taskRepository.search(
                status,
                priority,
                assigneeId,
                teamId,
                boardId,
                accessService.memberScopeOrNull(current),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<TaskResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID id) {
        Task task = findTask(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public KanbanResponse kanbanByBoard(UUID boardId) {
        Board board = boardRepository.findByIdWithTeam(boardId)
                .orElseThrow(() -> new BusinessException("Quadro não encontrado", HttpStatus.NOT_FOUND));
        AuthenticatedUser current = SecurityUtils.currentUser();
        accessService.ensureCanAccessTeam(current, board.getTeam());

        UUID onlyMine = current.isAdmin() ? null : current.getId();
        List<TaskResponse> tasks = taskRepository.findForKanban(
                        null,
                        boardId,
                        onlyMine,
                        accessService.memberScopeOrNull(current)
                ).stream()
                .map(this::toResponse)
                .toList();
        return buildKanban(board, tasks);
    }

    @Transactional(readOnly = true)
    public List<KanbanResponse> kanbanOverview(UUID teamId) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        if (teamId != null) {
            accessService.ensureCanAccessTeamId(current, teamId);
        }
        UUID onlyMine = current.isAdmin() ? null : current.getId();
        List<Task> tasks = taskRepository.findForKanban(
                teamId,
                null,
                onlyMine,
                accessService.memberScopeOrNull(current));
        Map<UUID, List<Task>> byBoard = tasks.stream()
                .collect(Collectors.groupingBy(t -> t.getBoard().getId()));
        return byBoard.values().stream()
                .map(boardTasks -> {
                    Board board = boardTasks.get(0).getBoard();
                    List<TaskResponse> responses = boardTasks.stream()
                            .map(this::toResponse)
                            .toList();
                    return buildKanban(board, responses);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Task getEntityForAccess(UUID id) {
        Task task = findTask(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());
        return task;
    }

    @Transactional(readOnly = true)
    public List<Task> lookupByQuery(String q) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        UUID assigneeScope = current.isAdmin() ? null : current.getId();
        String query = q == null ? "" : q.trim();
        if (query.isBlank()) {
            throw new BusinessException("Informe id ou nome da task", HttpStatus.BAD_REQUEST);
        }

        try {
            UUID id = UUID.fromString(query);
            Task task = findTask(id);
            ensureCanActOnOwnTask(current, task);
            return List.of(task);
        } catch (IllegalArgumentException ignored) {
            return taskRepository.findByTitleContainingIgnoreCaseScoped(query, assigneeScope);
        }
    }

    @Transactional
    public TaskResponse create(TaskRequest request) {
        Board board = boardRepository.findByIdWithTeam(request.boardId())
                .orElseThrow(() -> new BusinessException("Quadro não encontrado", HttpStatus.NOT_FOUND));
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), board.getTeam());

        User creator = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        User assignee = resolveAssignee(request.assigneeId());

        Task task = Task.builder()
                .title(request.title().trim())
                .description(request.description())
                .priority(request.priority() != null ? request.priority() : Priority.MEDIUM)
                .board(board)
                .assignee(assignee)
                .createdBy(creator)
                .dueDate(request.dueDate())
                .status(TaskStatus.PLANNING)
                .build();

        taskRepository.save(task);
        if (assignee != null) {
            whatsAppNotifier.notifyTaskAssigned(task);
        }
        return toResponse(task);
    }

    @Transactional
    public TaskResponse update(UUID id, TaskRequest request) {
        Task task = findTask(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());

        Board board = boardRepository.findByIdWithTeam(request.boardId())
                .orElseThrow(() -> new BusinessException("Quadro não encontrado", HttpStatus.NOT_FOUND));

        UUID previousAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
        User assignee = resolveAssignee(request.assigneeId());

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setBoard(board);
        task.setAssignee(assignee);
        task.setDueDate(request.dueDate());
        task.setUpdatedAt(LocalDateTime.now());

        UUID newAssigneeId = assignee != null ? assignee.getId() : null;
        if (assignee != null && !Objects.equals(previousAssigneeId, newAssigneeId)) {
            whatsAppNotifier.notifyTaskAssigned(task);
        }
        return toResponse(task);
    }

    @Transactional
    public void delete(UUID id) {
        Task task = findTask(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());
        taskRepository.delete(task);
    }

    @Transactional
    public TaskResponse changeStatus(UUID id, StatusUpdateRequest request) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        Task task = findTask(id);
        ensureCanActOnOwnTask(current, task);

        TaskStatus next = request.status();
        TaskStatus from = task.getStatus();
        if (!from.canTransitionTo(next)) {
            throw new BusinessException(
                    "Transição inválida de " + from + " para " + next,
                    HttpStatus.BAD_REQUEST);
        }
        if (next.requiresAssignee() && task.getAssignee() == null) {
            throw new BusinessException(
                    "Responsável é obrigatório para o status " + next,
                    HttpStatus.BAD_REQUEST);
        }

        applyStatusChange(task, from, next, current.getId());

        if (next == TaskStatus.AWAITING_REVIEW) {
            whatsAppNotifier.notifyAwaitingApproval(task);
        }
        return toResponse(task);
    }

    @Transactional
    public TaskResponse evaluate(UUID id, EvaluationRequest request) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        if (!current.isAdmin()) {
            throw new BusinessException("Somente ADMIN pode avaliar tarefas", HttpStatus.FORBIDDEN);
        }

        Task task = findTask(id);
        accessService.ensureCanAccessTeam(current, task.getBoard().getTeam());

        if (task.getStatus() != TaskStatus.IN_REVIEW) {
            throw new BusinessException(
                    "Tarefa precisa estar IN_REVIEW para ser avaliada",
                    HttpStatus.BAD_REQUEST);
        }
        if (task.getAssignee() == null) {
            throw new BusinessException("Tarefa precisa de responsável para ser avaliada", HttpStatus.BAD_REQUEST);
        }

        TaskStatus next = request.outcome() == EvaluationOutcome.APPROVED
                ? TaskStatus.COMPLETED
                : TaskStatus.REJECTED;

        TaskStatus from = task.getStatus();
        task.setRating(request.rating());
        task.setRatingDescription(request.comment());
        applyStatusChange(task, from, next, current.getId());
        whatsAppNotifier.notifyTaskEvaluated(task);
        return toResponse(task);
    }

    private void applyStatusChange(Task task, TaskStatus from, TaskStatus to, UUID actorId) {
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        TaskStatusLog log = TaskStatusLog.builder()
                .task(task)
                .user(actor)
                .fromStatus(from)
                .toStatus(to)
                .build();
        task.getStatusLogs().add(log);
        task.setStatus(to);
        task.setUpdatedAt(LocalDateTime.now());
    }

    private void ensureCanActOnOwnTask(AuthenticatedUser current, Task task) {
        accessService.ensureCanAccessTeam(current, task.getBoard().getTeam());
        if (current.isAdmin()) {
            return;
        }
        if (task.getAssignee() == null || !task.getAssignee().getId().equals(current.getId())) {
            throw new BusinessException("Você só pode alterar suas próprias tasks", HttpStatus.FORBIDDEN);
        }
    }

    private User resolveAssignee(UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return userRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException("Responsável não encontrado", HttpStatus.NOT_FOUND));
    }

    private Task findTask(UUID id) {
        return taskRepository.findByIdDetailed(id)
                .orElseThrow(() -> new BusinessException("Tarefa não encontrada", HttpStatus.NOT_FOUND));
    }

    private KanbanResponse buildKanban(Board board, List<TaskResponse> tasks) {
        Map<TaskStatus, List<TaskResponse>> columns = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            columns.put(status, tasks.stream().filter(t -> t.status() == status).toList());
        }
        return new KanbanResponse(
                board.getId(),
                board.getName(),
                board.getTeam().getId(),
                board.getTeam().getName(),
                board.getTeam().getColor(),
                columns);
    }

    public TaskResponse toResponse(Task task) {
        User assignee = task.getAssignee();
        User createdBy = task.getCreatedBy();
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getBoard().getId(),
                task.getBoard().getName(),
                task.getBoard().getTeam().getId(),
                task.getBoard().getTeam().getName(),
                task.getBoard().getTeam().getColor(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getName() : null,
                task.getCreatedAt(),
                task.getDueDate(),
                task.getUpdatedAt(),
                task.getRating(),
                task.getRatingDescription());
    }
}
