package com.example.ecommerce.modules.audit.service;

import com.example.ecommerce.modules.audit.dto.AuditLogResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.audit.entity.AuditAction;
import org.springframework.data.domain.Pageable;

/**
 * Writes and reads the audit trail (who did what, to which entity, from where).
 */
public interface AuditService {

    /** Records an action performed by the currently authenticated user. */
    void log(AuditAction action, String entityName, Long entityId, String details);

    /** Records an action for an explicit user (used before the security context is populated, e.g. login). */
    void log(AuditAction action, String entityName, Long entityId, String details, Long userId, String username);

    PageResponse<AuditLogResponse> search(String entityName, Long entityId, String username, Pageable pageable);
}
