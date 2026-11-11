package com.example.ecommerce.modules.user.entity;

import com.example.ecommerce.common.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.ecommerce.common.security.RoleName;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Login account for administrators and customers.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_enabled", columnList = "enabled")
})
public class User extends BaseEntity {

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 120)
    private String email;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "phone", length = 30)
    private String phone;

    /** BCrypt hash - never exposed through the API. */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * Session version. Incremented on password change or reset, account disabling and role changes.
     * Every signed-in session remembers the version it started with and is ended as soon as the
     * stored value differs (see SessionValidationFilter).
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** SHA-256 hash of the single-use password reset token. */
    @Column(name = "password_reset_token_hash", length = 64)
    private String passwordResetTokenHash;

    @Column(name = "password_reset_expires_at")
    private LocalDateTime passwordResetExpiresAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    public boolean hasRole(RoleName roleName) {
        return roles.stream().anyMatch(r -> r.getName() == roleName);
    }

    public void incrementTokenVersion() {
        this.tokenVersion++;
    }
}
