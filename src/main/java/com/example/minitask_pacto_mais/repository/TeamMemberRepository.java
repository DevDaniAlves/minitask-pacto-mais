package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.TeamMember;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository

public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    Page<TeamMember> findByTeamId(UUID teamId, Pageable pageable);

    Page<TeamMember> findByUserId(UUID userId, Pageable pageable);

    Page<TeamMember> findByTeamIdAndIsEnabledTrue(UUID teamId, Pageable pageable);

    Page<TeamMember> findByTeamIdAndIsEnabledFalse(UUID teamId, Pageable pageable); 

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);


}