package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.domain.TeamMember;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TeamMemberRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.BoardDtos.BoardResponse;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.BotCreateTaskRequest;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.IndexedBoard;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.IndexedUser;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.MeResponse;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.TaskDetailResponse;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.TaskLookupItem;
import com.example.minitask_pacto_mais.web.dtos.BotDtos.TaskLookupResponse;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.EvaluationRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.StatusUpdateRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BotService {

    private final UserRepository userRepository;
    private final BoardService boardService;
    private final BoardRepository boardRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TaskService taskService;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public MeResponse me() {
        AuthenticatedUser current = SecurityUtils.currentUser();
        User user = userRepository.findById(current.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));
        return new MeResponse(user.getId(), user.getName(), user.getEmail(), user.getPhone(), user.getRole());
    }

    @Transactional(readOnly = true)
    public List<IndexedBoard> listBoards() {
        List<BoardResponse> boards = boardService.list(null);
        List<IndexedBoard> indexed = new ArrayList<>();
        for (int i = 0; i < boards.size(); i++) {
            BoardResponse b = boards.get(i);
            indexed.add(new IndexedBoard(i + 1, b.id(), b.name(), b.teamId(), b.teamName()));
        }
        return indexed;
    }

    @Transactional(readOnly = true)
    public List<IndexedUser> listUsersForBoard(UUID boardId) {
        var board = boardRepository.findByIdWithTeam(boardId)
                .orElseThrow(() -> new BusinessException("Quadro não encontrado", HttpStatus.NOT_FOUND));
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), board.getTeam());

        List<TeamMember> members = teamMemberRepository.findByTeamIdAndIsEnabledTrue(board.getTeam().getId());
        List<IndexedUser> indexed = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            User u = members.get(i).getUser();
            indexed.add(new IndexedUser(i + 1, u.getId(), u.getName(), u.getEmail()));
        }
        return indexed;
    }

    @Transactional(readOnly = true)
    public TaskLookupResponse lookup(String q) {
        List<Task> tasks = taskService.lookupByQuery(q);
        if (tasks.isEmpty()) {
            throw new BusinessException("Nenhuma task encontrada", HttpStatus.NOT_FOUND);
        }
        if (tasks.size() == 1) {
            TaskDetailResponse detail = toDetail(tasks.get(0));
            String next = detail.allowedNextStatusLabels().isEmpty()
                    ? "sem próximos status"
                    : detail.allowedNextStatusLabels().toString();
            String hint = "Encontrei a task \"" + detail.title() + "\" (status "
                    + detail.currentStatusLabel() + "). Próximos status: " + next
                    + ". Confirme se é essa (sim/não) antes de alterar.";
            return new TaskLookupResponse(false, detail, List.of(), hint);
        }
        List<TaskLookupItem> matches = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            matches.add(new TaskLookupItem(
                    i + 1,
                    t.getId(),
                    t.getTitle(),
                    t.getStatus(),
                    t.getStatus().labelPt()));
        }
        String hint = "Encontrei " + matches.size()
                + " tasks. Peça o índice (1.." + matches.size()
                + ") para confirmar qual alterar.";
        return new TaskLookupResponse(true, null, matches, hint);
    }

    @Transactional(readOnly = true)
    public TaskDetailResponse getTask(UUID id) {
        return toDetail(taskService.getEntityForAccess(id));
    }

    @Transactional
    public TaskDetailResponse changeStatus(UUID id, StatusUpdateRequest request) {
        TaskResponse response = taskService.changeStatus(id, request);
        return toDetail(taskService.getEntityForAccess(response.id()));
    }

    @Transactional
    public TaskDetailResponse evaluate(UUID id, EvaluationRequest request) {
        TaskResponse response = taskService.evaluate(id, request);
        return toDetail(taskService.getEntityForAccess(response.id()));
    }

    @Transactional
    public TaskDetailResponse createTask(BotCreateTaskRequest request) {
        UUID boardId = resolveBoardId(request);
        UUID assigneeId = resolveAssigneeId(request, boardId);

        TaskRequest taskRequest = new TaskRequest(
                request.title(),
                request.description(),
                request.priority() != null ? request.priority() : Priority.MEDIUM,
                boardId,
                assigneeId,
                request.dueDate()
        );
        TaskResponse created = taskService.create(taskRequest);
        return toDetail(taskService.getEntityForAccess(created.id()));
    }

    private UUID resolveBoardId(BotCreateTaskRequest request) {
        if (request.boardId() != null) {
            return request.boardId();
        }
        if (request.boardIndex() == null) {
            throw new BusinessException("Informe boardId ou boardIndex", HttpStatus.BAD_REQUEST);
        }
        List<IndexedBoard> boards = listBoards();
        return pickByIndex(boards, request.boardIndex(), "board").id();
    }

    private UUID resolveAssigneeId(BotCreateTaskRequest request, UUID boardId) {
        if (request.assigneeId() != null) {
            return request.assigneeId();
        }
        if (request.assigneeIndex() == null || request.assigneeIndex() < 1) {
            return null;
        }
        List<IndexedUser> users = listUsersForBoard(boardId);
        return pickByIndex(users, request.assigneeIndex(), "usuário").id();
    }

    private <T> T pickByIndex(List<T> items, int index, String label) {
        if (index < 1 || index > items.size()) {
            throw new BusinessException(
                    "Índice de " + label + " inválido. Use 1.." + items.size(),
                    HttpStatus.BAD_REQUEST
            );
        }
        return items.get(index - 1);
    }

    private TaskDetailResponse toDetail(Task task) {
        List<TaskStatus> next = List.copyOf(task.getStatus().allowedTransitions());
        List<String> nextLabels = next.stream().map(TaskStatus::labelPt).toList();
        return new TaskDetailResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getStatus().labelPt(),
                next,
                nextLabels,
                task.getPriority(),
                task.getBoard().getId(),
                task.getBoard().getName(),
                task.getBoard().getTeam().getId(),
                task.getBoard().getTeam().getName(),
                task.getAssignee() != null ? task.getAssignee().getId() : null,
                task.getAssignee() != null ? task.getAssignee().getName() : null,
                task.getDueDate(),
                task.getRating()
        );
    }
}
