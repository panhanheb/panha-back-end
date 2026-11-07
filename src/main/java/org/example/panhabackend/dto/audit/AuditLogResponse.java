package org.example.panhabackend.dto.audit;

import org.example.panhabackend.enums.AuditAction;

import java.time.LocalDateTime;

public record AuditLogResponse(Long id,
                               Long userId,
                               String username,
                               AuditAction action,
                               String entityName,
                               Long entityId,
                               String details,
                               String ipAddress,
                               LocalDateTime createdAt) {
}
