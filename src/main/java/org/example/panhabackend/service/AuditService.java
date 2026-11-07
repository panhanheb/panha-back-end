package org.example.panhabackend.service;

import org.example.panhabackend.dto.audit.AuditLogResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.enums.AuditAction;
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
