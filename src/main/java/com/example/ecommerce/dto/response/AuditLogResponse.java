package com.example.ecommerce.dto.response;

import com.example.ecommerce.model.enums.AuditAction;

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
