package com.example.ecommerce.dto.response;

import com.example.ecommerce.model.enums.RoleName;

import java.util.Set;

/**
 * What the token services need to know about an account. Never contains the password hash.
 *
 * @param sessionVersion current session version; tokens carrying another value are rejected
 */
public record AccountView(Long id, String username, String email, Set<RoleName> roles, int sessionVersion) {
}
