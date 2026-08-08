package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.TaskStatusLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.minitask_pacto_mais.domain.TaskStatus;

@Repository

public interface TaskStatusLogRepository extends JpaRepository<TaskStatusLog, UUID> {

    Page<TaskStatusLog> findByTaskId(UUID taskId, Pageable pageable);

    Page<TaskStatusLog> findByUserId(UUID userId, Pageable pageable);

    Page<TaskStatusLog> findByFromStatus(TaskStatus fromStatus, Pageable pageable);

    Page<TaskStatusLog> findByToStatus(TaskStatus toStatus, Pageable pageable);


}