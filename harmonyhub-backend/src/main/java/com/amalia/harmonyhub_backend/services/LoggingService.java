package com.amalia.harmonyhub_backend.services;

import com.amalia.harmonyhub_backend.model.AuditLog;
import com.amalia.harmonyhub_backend.model.User;
import com.amalia.harmonyhub_backend.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoggingService {
    @Autowired
    private AuditLogRepository auditLogRepository;

    public AuditLog record(User user, String actionDescription) {
        String roleName = user.getRoles().stream()
                .anyMatch(r -> r.getName().equals("ROLE_ADMIN")) ? "ADMIN" : "USER";

        AuditLog log = new AuditLog(String.valueOf(user.getId()), roleName, actionDescription);
        return auditLogRepository.save(log);
    }
    public List<AuditLog> getAllLogs() {
        return auditLogRepository.findAll();
    }
}
