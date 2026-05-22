package com.securebank.service;

import com.securebank.dto.AuditLogResponse;
import com.securebank.model.AuditAction;
import com.securebank.model.AuditLog;
import com.securebank.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void record(AuditAction action, String actorEmail, String targetType, Long targetId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .actorEmail(actorEmail)
                .targetType(targetType)
                .targetId(targetId)
                .details(truncate(details))
                .build();

        auditLogRepository.save(auditLog);
    }

    public List<AuditLogResponse> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    private String truncate(String details) {
        if (details == null || details.length() <= 500) {
            return details;
        }
        return details.substring(0, 500);
    }
}
