package com.example.ecommerce.modules.user.dto;

import com.example.ecommerce.common.security.RoleName;

import java.util.Set;

/**
 * What other modules may know about an account to issue tokens for it. Never contains the password hash.
 *
 * @param sessionVersion current session version; tokens carrying another value are rejected
 */
public record AccountView(Long id, String username, String email, Set<RoleName> roles, int sessionVersion) {
}
