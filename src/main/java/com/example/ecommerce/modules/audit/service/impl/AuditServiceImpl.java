package com.example.ecommerce.modules.audit.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.ecommerce.modules.audit.dto.AuditLogResponse;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.audit.entity.AuditLog;
import com.example.ecommerce.modules.audit.entity.AuditAction;
import com.example.ecommerce.modules.audit.repository.AuditLogRepository;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.common.security.UserPrincipal;
import com.example.ecommerce.modules.audit.service.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int MAX_DETAILS_LENGTH = 2000;

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void log(AuditAction action, String entityName, Long entityId, String details) {
        Optional<UserPrincipal> principal = SecurityUtils.currentPrincipal();
        log(action, entityName, entityId, details,
                principal.map(UserPrincipal::getId).orElse(null),
                principal.map(UserPrincipal::getUsername).orElse(null));
    }

    @Override
    @Transactional
    public void log(AuditAction action, String entityName, Long entityId, String details, Long userId, String username) {
        AuditLog entry = new AuditLog();
        entry.setAction(action);
        entry.setEntityName(entityName);
        entry.setEntityId(entityId);
        entry.setDetails(truncate(details));
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setIpAddress(currentIpAddress());
        auditLogRepository.save(entry);
        log.info("AUDIT {} {}#{} by {} - {}", action, entityName, entityId, username, details);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String entityName, Long entityId, String username, Pageable pageable) {
        return PageResponse.from(
                auditLogRepository.search(blankToNull(entityName), entityId, blankToNull(username), pageable),
                a -> new AuditLogResponse(a.getId(), a.getUserId(), a.getUsername(), a.getAction(),
                        a.getEntityName(), a.getEntityId(), a.getDetails(), a.getIpAddress(), a.getCreatedAt()));
    }

    private static String currentIpAddress() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = StringUtils.hasText(forwarded) ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
        return ip != null && ip.length() > 45 ? ip.substring(0, 45) : ip;
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_DETAILS_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DETAILS_LENGTH);
    }

    private static String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
