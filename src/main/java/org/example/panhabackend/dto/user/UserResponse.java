package org.example.panhabackend.dto.user;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Public view of an account. Never contains the password hash or reset tokens.
 */
public record UserResponse(Long id,
                           String username,
                           String email,
                           boolean enabled,
                           Set<String> roles,
                           Long teacherId,
                           Long studentId,
                           LocalDateTime lastLoginAt,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
}
