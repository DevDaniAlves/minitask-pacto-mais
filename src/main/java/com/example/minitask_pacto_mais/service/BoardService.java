package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Board;
import com.example.minitask_pacto_mais.domain.Team;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TaskRepository;
import com.example.minitask_pacto_mais.repository.TeamMemberRepository;
import com.example.minitask_pacto_mais.repository.TeamRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.BoardDtos.BoardRequest;
import com.example.minitask_pacto_mais.web.dtos.BoardDtos.BoardResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public List<BoardResponse> list(UUID teamId) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        if (teamId != null) {
            accessService.ensureCanAccessTeamId(current, teamId);
            return boardRepository.findByTeamIdWithTeam(teamId).stream().map(this::toResponse).toList();
        }
        if (current.isAdmin()) {
            return boardRepository.findAllWithTeam().stream().map(this::toResponse).toList();
        }
        Set<UUID> teamIds = teamMemberRepository.findByUserIdAndIsEnabledTrue(current.getId()).stream()
                .map(tm -> tm.getTeam().getId())
                .collect(Collectors.toSet());
        return boardRepository.findAllWithTeam().stream()
                .filter(board -> teamIds.contains(board.getTeam().getId()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BoardResponse get(UUID id) {
        Board board = findBoard(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), board.getTeam());
        return toResponse(board);
    }

    @Transactional
    public BoardResponse create(BoardRequest request) {
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new BusinessException("Time não encontrado", HttpStatus.NOT_FOUND));
        User creator = userRepository.findById(SecurityUtils.currentUser().getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Board board = Board.builder()
                .name(request.name().trim())
                .team(team)
                .createdBy(creator)
                .build();
        return toResponse(boardRepository.save(board));
    }

    @Transactional
    public BoardResponse update(UUID id, BoardRequest request) {
        Board board = findBoard(id);
        Team team = teamRepository.findById(request.teamId())
                .orElseThrow(() -> new BusinessException("Time não encontrado", HttpStatus.NOT_FOUND));
        board.setName(request.name().trim());
        board.setTeam(team);
        board.setUpdatedAt(LocalDateTime.now());
        return toResponse(board);
    }

    @Transactional
    public void delete(UUID id) {
        Board board = findBoard(id);
        if (taskRepository.existsByBoardId(id)) {
            throw new BusinessException(
                    "Remova as tarefas do quadro antes de excluí-lo",
                    HttpStatus.CONFLICT
            );
        }
        boardRepository.delete(board);
    }

    private Board findBoard(UUID id) {
        return boardRepository.findByIdWithTeam(id)
                .orElseThrow(() -> new BusinessException("Quadro não encontrado", HttpStatus.NOT_FOUND));
    }

    private BoardResponse toResponse(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getName(),
                board.getTeam().getId(),
                board.getTeam().getName(),
                board.getTeam().getColor()
        );
    }
}
