package com.example.minitask_pacto_mais.service;

import com.example.minitask_pacto_mais.domain.Team;
import com.example.minitask_pacto_mais.repository.TeamMemberRepository;
import com.example.minitask_pacto_mais.security.AuthenticatedUser;
import com.example.minitask_pacto_mais.web.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccessService {

    private final TeamMemberRepository teamMemberRepository;

    public AccessService(TeamMemberRepository teamMemberRepository) {
        this.teamMemberRepository = teamMemberRepository;
    }

    public void ensureCanAccessTeam(AuthenticatedUser user, Team team) {
        ensureCanAccessTeamId(user, team.getId());
    }

    public void ensureCanAccessTeamId(AuthenticatedUser user, UUID teamId) {
        if (user.isAdmin()) {
            return;
        }
        if (!teamMemberRepository.existsByTeamIdAndUserIdAndIsEnabledTrue(teamId, user.getId())) {
            throw new BusinessException("Você não é membro deste time", HttpStatus.FORBIDDEN);
        }
    }

    public UUID memberScopeOrNull(AuthenticatedUser user) {
        return user.isAdmin() ? null : user.getId();
    }
}