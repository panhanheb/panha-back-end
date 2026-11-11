package com.example.ecommerce.dto.response;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Public view of an account. Never contains the password hash or reset tokens.
 */
public record UserResponse(Long id,
                           String username,
                           String email,
                           String fullName,
                           String phone,
                           boolean enabled,
                           Set<String> roles,
                           LocalDateTime lastLoginAt,
                           LocalDateTime createdAt,
                           LocalDateTime updatedAt) {
}
