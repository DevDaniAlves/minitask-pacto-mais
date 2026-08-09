package com.example.minitask_pacto_mais.repository;

import com.example.minitask_pacto_mais.domain.TeamMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Page<TeamMember> findByTeamId(UUID teamId, Pageable pageable);

    Page<TeamMember> findByUserId(UUID userId, Pageable pageable);

    Page<TeamMember> findByTeamIdAndIsEnabledTrue(UUID teamId, Pageable pageable);

    Page<TeamMember> findByTeamIdAndIsEnabledFalse(UUID teamId, Pageable pageable);

    List<TeamMember> findByUserIdAndIsEnabledTrue(UUID userId);

    List<TeamMember> findByTeamIdAndIsEnabledTrue(UUID teamId);

    List<TeamMember> findByTeamId(UUID teamId);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserIdAndIsEnabledTrue(UUID teamId, UUID userId);
}
