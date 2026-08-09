package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.Team;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Page<Team> findByName(String name, Pageable pageable);

    
    
}