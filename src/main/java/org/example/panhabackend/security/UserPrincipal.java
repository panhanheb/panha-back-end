package org.example.panhabackend.security;

import lombok.Getter;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.RoleName;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security view of a {@link User}. Immutable snapshot taken when the request is authenticated.
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final int tokenVersion;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.tokenVersion = user.getTokenVersion();
        this.authorities = user.getRoles().stream()
                .map(r -> (GrantedAuthority) new SimpleGrantedAuthority(r.getName().name()))
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
