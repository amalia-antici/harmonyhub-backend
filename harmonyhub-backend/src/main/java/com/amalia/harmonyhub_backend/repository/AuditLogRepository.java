package com.amalia.harmonyhub_backend.repository;

import com.amalia.harmonyhub_backend.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(String userId);
    long countByUserIdAndActionAndTimestampAfter(
            String userId,
            String action,
            LocalDateTime timestamp
    );
    List<AuditLog> findAllByUserIdAndTimestampAfter(String userId, LocalDateTime timestamp);
}
