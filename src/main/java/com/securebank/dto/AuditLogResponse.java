package com.securebank.dto;

import com.securebank.model.AuditAction;
import com.securebank.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private AuditAction action;
    private String actorEmail;
    private String targetType;
    private Long targetId;
    private String details;
    private Instant createdAt;

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getActorEmail(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDetails(),
                auditLog.getCreatedAt()
        );
    }
}
