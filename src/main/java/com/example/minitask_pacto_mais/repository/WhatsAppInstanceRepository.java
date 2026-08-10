package com.example.minitask_pacto_mais.repository;

import com.example.minitask_pacto_mais.domain.WhatsAppConnectionStatus;
import com.example.minitask_pacto_mais.domain.WhatsAppInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WhatsAppInstanceRepository extends JpaRepository<WhatsAppInstance, UUID> {

    boolean existsByInstanceNameIgnoreCase(String instanceName);

    Optional<WhatsAppInstance> findByInstanceNameIgnoreCase(String instanceName);

    Optional<WhatsAppInstance> findFirstByActiveTrueAndStatus(WhatsAppConnectionStatus status);

    List<WhatsAppInstance> findAllByOrderByCreatedAtDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WhatsAppInstance w SET w.active = false WHERE w.active = true")
    void clearActiveFlags();
}
