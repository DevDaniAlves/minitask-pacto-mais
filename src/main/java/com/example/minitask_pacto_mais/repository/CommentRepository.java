package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.Comment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    Page<Comment> findByTaskId(UUID taskId, Pageable pageable);

    Page<Comment> findByAuthorId(UUID author, Pageable pageable);

    Page<Comment> findByTaskIdAndDeletedAtIsNull(UUID taskId, Pageable pageable);

    List<Comment> findByTaskIdAndDeletedAtIsNullOrderByCreatedAtAsc(UUID taskId);

}