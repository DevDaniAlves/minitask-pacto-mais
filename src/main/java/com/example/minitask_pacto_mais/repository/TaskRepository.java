package com.example.minitask_pacto_mais.repository;

import com.example.minitask_pacto_mais.domain.Priority;
import com.example.minitask_pacto_mais.domain.Task;
import com.example.minitask_pacto_mais.domain.TaskStatus;
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
public interface TaskRepository extends JpaRepository<Task, UUID> {

    Page<Task> findByBoardId(UUID boardId, Pageable pageable);

    Page<Task> findByCreatedById(UUID createdById, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByAssigneeId(UUID assignee, Pageable pageable);

    Page<Task> findByPriority(Priority priority, Pageable pageable);

    boolean existsByBoardId(UUID boardId);

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.board b
            JOIN FETCH b.team
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.createdBy
            WHERE t.id = :id
            """)
    Optional<Task> findByIdDetailed(@Param("id") UUID id);

    @Query(
            value = """
                    SELECT t FROM Task t
                    JOIN t.board b
                    JOIN b.team team
                    LEFT JOIN t.assignee a
                    WHERE (:status IS NULL OR t.status = :status)
                      AND (:priority IS NULL OR t.priority = :priority)
                      AND (:assigneeId IS NULL OR a.id = :assigneeId)
                      AND (:teamId IS NULL OR team.id = :teamId)
                      AND (:boardId IS NULL OR b.id = :boardId)
                      AND (:memberUserId IS NULL OR EXISTS (
                            SELECT 1 FROM TeamMember tm
                            WHERE tm.team = team
                              AND tm.user.id = :memberUserId
                              AND tm.isEnabled = true
                      ))
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Task t
                    JOIN t.board b
                    JOIN b.team team
                    LEFT JOIN t.assignee a
                    WHERE (:status IS NULL OR t.status = :status)
                      AND (:priority IS NULL OR t.priority = :priority)
                      AND (:assigneeId IS NULL OR a.id = :assigneeId)
                      AND (:teamId IS NULL OR team.id = :teamId)
                      AND (:boardId IS NULL OR b.id = :boardId)
                      AND (:memberUserId IS NULL OR EXISTS (
                            SELECT 1 FROM TeamMember tm
                            WHERE tm.team = team
                              AND tm.user.id = :memberUserId
                              AND tm.isEnabled = true
                      ))
                    """
    )
    Page<Task> search(
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assigneeId") UUID assigneeId,
            @Param("teamId") UUID teamId,
            @Param("boardId") UUID boardId,
            @Param("memberUserId") UUID memberUserId,
            Pageable pageable
    );

    @Query("""
            SELECT t FROM Task t
            JOIN FETCH t.board b
            JOIN FETCH b.team team
            LEFT JOIN FETCH t.assignee
            LEFT JOIN FETCH t.createdBy
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :title, '%'))
              AND (:memberUserId IS NULL OR EXISTS (
                    SELECT 1 FROM TeamMember tm
                    WHERE tm.team = team
                      AND tm.user.id = :memberUserId
                      AND tm.isEnabled = true
              ))
            ORDER BY t.createdAt DESC
            """)
    List<Task> findByTitleContainingIgnoreCaseScoped(
            @Param("title") String title,
            @Param("memberUserId") UUID memberUserId
    );
}
