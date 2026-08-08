package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.Board;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository

public interface BoardRepository extends JpaRepository<Board, UUID> {

    Page<Board> findByCreatedById(UUID createdById, Pageable pageable);

    Page<Board> findByTeamId(UUID teamId, Pageable pageable);

    Page<Board> findByName(String name, Pageable pageable);

}