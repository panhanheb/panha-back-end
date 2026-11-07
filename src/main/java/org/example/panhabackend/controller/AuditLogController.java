package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.audit.AuditLogResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.service.AuditService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "12. Audit logs", description = "Audit trail of changes (ADMIN)")
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @Operation(summary = "Search the audit trail by entity and/or username")
    @GetMapping
    public PageResponse<AuditLogResponse> search(@RequestParam(required = false) String entityName,
                                                 @RequestParam(required = false) Long entityId,
                                                 @RequestParam(required = false) String username,
                                                 @ParameterObject
                                                 @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
                                                 Pageable pageable) {
        return auditService.search(entityName, entityId, username, pageable);
    }
}
