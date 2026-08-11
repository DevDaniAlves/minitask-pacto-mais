package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Board;
import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Role;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.domain.Team;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.notification.WhatsAppNotifier;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TaskRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.StatusUpdateRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskRequest;
import com.example.minitask_pacto_mais.web.dtos.TaskDtos.TaskResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccessService accessService;
    @Mock
    private WhatsAppNotifier whatsAppNotifier;

    @InjectMocks
    private TaskService taskService;

    private UUID adminId;
    private UUID boardId;
    private UUID assigneeId;
    private User admin;
    private User assignee;
    private Team team;
    private Board board;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        boardId = UUID.randomUUID();
        assigneeId = UUID.randomUUID();

        admin = User.builder()
                .id(adminId)
                .name("Admin")
                .email("admin@demo.com")
                .role(Role.ADMIN)
                .passwordHash("x")
                .build();
        assignee = User.builder()
                .id(assigneeId)
                .name("Func")
                .email("func@demo.com")
                .role(Role.FUNCIONARIO)
                .passwordHash("x")
                .build();
        team = Team.builder()
                .id(UUID.randomUUID())
                .name("Time Alpha")
                .color("#2563EB")
                .build();
        board = Board.builder()
                .id(boardId)
                .name("Kanban")
                .team(team)
                .build();

        AuthenticatedUser principal = new AuthenticatedUser(
                adminId, admin.getEmail(), admin.getName(), "x", Role.ADMIN);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createSemResponsavelFicaEmPlanning() {
        when(boardRepository.findByIdWithTeam(boardId)).thenReturn(Optional.of(board));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse response = taskService.create(new TaskRequest(
                "Limpeza",
                "desc",
                Priority.HIGH,
                boardId,
                null,
                null
        ));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals(TaskStatus.PLANNING, captor.getValue().getStatus());
        assertEquals(TaskStatus.PLANNING, response.status());
        verify(whatsAppNotifier, never()).notifyTaskAssigned(any());
    }

    @Test
    void createComResponsavelFicaEmAssignedENotifica() {
        when(boardRepository.findByIdWithTeam(boardId)).thenReturn(Optional.of(board));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));
        when(userRepository.findById(assigneeId)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        TaskResponse response = taskService.create(new TaskRequest(
                "Limpeza",
                "desc",
                Priority.MEDIUM,
                boardId,
                assigneeId,
                null
        ));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals(TaskStatus.ASSIGNED, captor.getValue().getStatus());
        assertEquals(assigneeId, captor.getValue().getAssignee().getId());
        assertEquals(TaskStatus.ASSIGNED, response.status());
        verify(whatsAppNotifier).notifyTaskAssigned(any(Task.class));
    }

    @Test
    void changeStatusTransicaoValida() {
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .title("Limpeza")
                .status(TaskStatus.ASSIGNED)
                .priority(Priority.MEDIUM)
                .board(board)
                .assignee(assignee)
                .createdBy(admin)
                .statusLogs(new ArrayList<>())
                .build();
        when(taskRepository.findByIdDetailed(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        TaskResponse response = taskService.changeStatus(taskId, new StatusUpdateRequest(TaskStatus.IN_PROGRESS));

        assertEquals(TaskStatus.IN_PROGRESS, task.getStatus());
        assertEquals(TaskStatus.IN_PROGRESS, response.status());
        assertEquals(1, task.getStatusLogs().size());
    }

    @Test
    void changeStatusTransicaoInvalidaFalha() {
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .title("Limpeza")
                .status(TaskStatus.PLANNING)
                .priority(Priority.MEDIUM)
                .board(board)
                .assignee(null)
                .createdBy(admin)
                .statusLogs(new ArrayList<>())
                .build();
        when(taskRepository.findByIdDetailed(taskId)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                taskService.changeStatus(taskId, new StatusUpdateRequest(TaskStatus.COMPLETED)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(TaskStatus.PLANNING, task.getStatus());
    }

    @Test
    void changeStatusParaAssignedSemResponsavelFalha() {
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .title("Limpeza")
                .status(TaskStatus.PLANNING)
                .priority(Priority.MEDIUM)
                .board(board)
                .assignee(null)
                .createdBy(admin)
                .statusLogs(new ArrayList<>())
                .build();
        when(taskRepository.findByIdDetailed(taskId)).thenReturn(Optional.of(task));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                taskService.changeStatus(taskId, new StatusUpdateRequest(TaskStatus.ASSIGNED)));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(TaskStatus.PLANNING, task.getStatus());
    }

    @Test
    void changeStatusParaCompletedComResponsavel() {
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .title("Limpeza")
                .status(TaskStatus.IN_REVIEW)
                .priority(Priority.MEDIUM)
                .board(board)
                .assignee(assignee)
                .createdBy(admin)
                .statusLogs(new ArrayList<>())
                .build();
        when(taskRepository.findByIdDetailed(taskId)).thenReturn(Optional.of(task));
        when(userRepository.findById(adminId)).thenReturn(Optional.of(admin));

        TaskResponse response = taskService.changeStatus(taskId, new StatusUpdateRequest(TaskStatus.COMPLETED));

        assertEquals(TaskStatus.COMPLETED, response.status());
        assertEquals(TaskStatus.COMPLETED, task.getStatus());
    }
}
