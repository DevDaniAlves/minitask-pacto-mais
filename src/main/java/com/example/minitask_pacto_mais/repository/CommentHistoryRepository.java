package com.example.minitask_pacto_mais.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.minitask_pacto_mais.domain.CommentHistory;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository

public interface CommentHistoryRepository extends JpaRepository<CommentHistory, UUID> {

    Page<CommentHistory> findByCommentId(UUID commentId, Pageable pageable);

    Page<CommentHistory> findByChangedById(UUID changedBy, Pageable pageable);

}