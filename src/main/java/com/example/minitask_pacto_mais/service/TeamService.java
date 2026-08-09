package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Team;
import com.example.minitask_pacto_mais.domain.TeamMember;
import com.example.minitask_pacto_mais.domain.User;
import com.example.minitask_pacto_mais.repository.BoardRepository;
import com.example.minitask_pacto_mais.repository.TeamMemberRepository;
import com.example.minitask_pacto_mais.repository.TeamRepository;
import com.example.minitask_pacto_mais.repository.UserRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.security.SecurityUtils;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.MemberResponse;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.TeamRequest;
import com.example.minitask_pacto_mais.web.dtos.TeamDtos.TeamResponse;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final AccessService accessService;

    @Transactional(readOnly = true)
    public List<TeamResponse> list() {
        AuthenticatedUser current = SecurityUtils.currentUser();
        List<Team> teams = current.isAdmin()
                ? teamRepository.findAll()
                : teamMemberRepository.findByUserIdAndIsEnabledTrue(current.getId()).stream()
                .map(TeamMember::getTeam)
                .toList();
        return teams.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse get(UUID id) {
        Team team = findTeam(id);
        accessService.ensureCanAccessTeam(SecurityUtils.currentUser(), team);
        return toResponse(team);
    }

    @Transactional
    public TeamResponse create(TeamRequest request) {
        AuthenticatedUser current = SecurityUtils.currentUser();
        User creator = userRepository.findById(current.getId())
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        Team team = Team.builder()
                .name(request.name().trim())
                .color(request.color().trim().toUpperCase())
                .createdBy(creator)
                .build();
        return toResponse(teamRepository.save(team));
    }

    @Transactional
    public TeamResponse update(UUID id, TeamRequest request) {
        Team team = findTeam(id);
        team.setName(request.name().trim());
        team.setColor(request.color().trim().toUpperCase());
        return toResponse(team);
    }

    @Transactional
    public void delete(UUID id) {
        Team team = findTeam(id);
        if (!boardRepository.findByTeamId(id).isEmpty()) {
            throw new BusinessException(
                    "Remova os quadros do time antes de excluí-lo",
                    HttpStatus.CONFLICT
            );
        }
        teamMemberRepository.findByTeamId(id).forEach(teamMemberRepository::delete);
        teamRepository.delete(team);
    }

    @Transactional
    public TeamResponse addMember(UUID teamId, UUID userId) {
        Team team = findTeam(teamId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("Usuário não encontrado", HttpStatus.NOT_FOUND));

        TeamMember existing = teamMemberRepository.findByTeamIdAndUserId(teamId, userId).orElse(null);
        if (existing != null) {
            if (existing.isEnabled()) {
                throw new BusinessException("Usuário já é membro do time", HttpStatus.CONFLICT);
            }
            existing.setEnabled(true);
            existing.setLeftAt(null);
            if (existing.getJoinedAt() == null) {
                existing.setJoinedAt(LocalDateTime.now());
            }
            return toResponse(team);
        }

        teamMemberRepository.save(TeamMember.builder()
                .team(team)
                .user(user)
                .isEnabled(true)
                .build());
        return toResponse(team);
    }

    @Transactional
    public TeamResponse removeMember(UUID teamId, UUID userId) {
        Team team = findTeam(teamId);
        TeamMember member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
                .orElseThrow(() -> new BusinessException("Membro não encontrado neste time", HttpStatus.NOT_FOUND));

        if (!member.isEnabled()) {
            throw new BusinessException("Membro já está desativado neste time", HttpStatus.CONFLICT);
        }

        member.setEnabled(false);
        member.setLeftAt(LocalDateTime.now());
        return toResponse(team);
    }

    private Team findTeam(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Time não encontrado", HttpStatus.NOT_FOUND));
    }

    private TeamResponse toResponse(Team team) {
        List<MemberResponse> members = teamMemberRepository.findByTeamIdAndIsEnabledTrue(team.getId()).stream()
                .map(tm -> new MemberResponse(
                        tm.getUser().getId(),
                        tm.getUser().getName(),
                        tm.getUser().getEmail(),
                        tm.isEnabled()
                ))
                .toList();
        return new TeamResponse(team.getId(), team.getName(), team.getColor(), members);
    }
}
