package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.AuditLog;
import com.amalia.harmonyhub_backend.model.SuspiciousUser;
import com.amalia.harmonyhub_backend.repository.AuditLogRepository;
import com.amalia.harmonyhub_backend.repository.SuspiciousUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BehaviorMonitorService {
    @Autowired
    private SuspiciousUserRepository suspiciousRepo;

    @Autowired
    private AuditLogRepository auditRepo;

    @Async
    public void analyzeLog(AuditLog log) {
        if (log.getAction() != null && log.getAction().contains("DELETE")) {

            List<AuditLog> recentLogs = auditRepo.findAllByUserIdAndTimestampAfter(
                    log.getUserId(),
                    LocalDateTime.now().minusMinutes(2)
            );

            long deleteCount = recentLogs.stream()
                    .filter(l -> l.getAction() != null && l.getAction().contains("DELETE"))
                    .count();

            System.out.println(">>> DELETE COUNT for user " + log.getUserId() + ": " + deleteCount);

            if (deleteCount > 2) {
                flagUser(log, "Mass deletion detected (" + deleteCount + " events)", "HIGH");
            }
        }
    }

    private void flagUser(AuditLog log, String reason, String severity) {
        SuspiciousUser entry = new SuspiciousUser();
        entry.setUserId(String.valueOf(log.getUserId()));
        entry.setReason(reason);
        entry.setSeverity(severity);
        entry.setTimestamp(LocalDateTime.now());
        suspiciousRepo.save(entry);
    }
    public List<SuspiciousUser> getObservationList() {
        return suspiciousRepo.findAll();
    }
}