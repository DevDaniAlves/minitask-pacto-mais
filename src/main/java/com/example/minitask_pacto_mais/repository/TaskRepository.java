package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.Task;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.minitask_pacto_mais.domain.TaskStatus;
import com.example.minitask_pacto_mais.domain.Priority;

@Repository

public interface TaskRepository extends JpaRepository<Task, UUID> {

    
    Page<Task> findByBoardId(UUID boardId, Pageable pageable);

    Page<Task> findByCreatedById(UUID createdById, Pageable pageable);

    Page<Task> findByStatus(TaskStatus status, Pageable pageable);

    Page<Task> findByAssigneeId(UUID assignee, Pageable pageable);

    Page<Task> findByPriority(Priority priority, Pageable pageable);

}