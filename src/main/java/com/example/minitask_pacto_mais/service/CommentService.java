package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Comment;
import com.example.minitask_pacto_mais.domain.CommentHistory;
import com.example.minitask_pacto_mais.domain.CommentHistoryAction;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.CommentHistoryRepository;
import com.example.minitask_pacto_mais.repository.CommentRepository;
import com.example.minitask_pacto_mais.repository.TaskRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.CommentDtos.CommentRequest;
import com.example.minitask_pacto_mais.web.dtos.CommentDtos.CommentResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentHistoryRepository commentHistoryRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public List<CommentResponse> listByTask(UUID taskId) {
        Task task = findTask(taskId);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());

        return commentRepository
                .findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(taskId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse create(UUID taskId, CommentRequest request) {
        Task task = findTask(taskId);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), task.getBoard().getTeam());

        User author = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Comment comment = Comment.builder()
                .task(task)
                .author(author)
                .body(request.body().trim())
                .build();
        commentRepository.save(comment);

        commentHistoryRepository.save(CommentHistory.builder()
                .comment(comment)
                .action(CommentHistoryAction.CREATED)
                .previousBody(null)
                .newBody(comment.getBody())
                .changedBy(author)
                .build());

        return toResponse(comment);
    }

    private Task findTask(UUID taskId) {
        return taskRepository.findByIdDetailed(taskId)
                .orElseThrow(() -> new BusinessException("Tarefa não encontrada", HttpStatus.NOT_FOUND));
    }

    private CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}