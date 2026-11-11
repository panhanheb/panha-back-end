package com.example.ecommerce.security;

import com.example.ecommerce.model.enums.RoleName;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The authenticated user as controllers and services see it. An immutable snapshot; for API calls it is
 * built from the access token, so authenticated requests need no user lookup beyond the session version.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    /** Session/token version of the account when this principal was built (see users.token_version). */
    private final int tokenVersion;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String password, boolean enabled, int tokenVersion,
                         Collection<RoleName> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.tokenVersion = tokenVersion;
        this.authorities = roles.stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.name()))
                .toList();
    }

    public boolean hasRole(RoleName role) {
        return authorities.stream().anyMatch(a -> role.name().equals(a.getAuthority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
