package com.example.ecommerce.modules.audit.dto;

import com.example.ecommerce.modules.audit.service.AuditAction;

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
