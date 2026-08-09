package com.example.minitask_pacto_mais.repository;

import com.example.minitask_pacto_mais.domain.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BoardRepository extends JpaRepository<Board, UUID> {

    Page<Board> findByCreatedById(UUID createdById, Pageable pageable);

    Page<Board> findByTeamId(UUID teamId, Pageable pageable);

    Page<Board> findByName(String name, Pageable pageable);

    List<Board> findByTeamId(UUID teamId);

    @Query("""
            SELECT b FROM Board b
            JOIN FETCH b.team
            WHERE b.id = :id
            """)
    Optional<Board> findByIdWithTeam(@Param("id") UUID id);

    @Query("""
            SELECT b FROM Board b
            JOIN FETCH b.team
            WHERE b.team.id = :teamId
            ORDER BY b.createdAt DESC
            """)
    List<Board> findByTeamIdWithTeam(@Param("teamId") UUID teamId);

    @Query("""
            SELECT b FROM Board b
            JOIN FETCH b.team
            ORDER BY b.createdAt DESC
            """)
    List<Board> findAllWithTeam();
}
