package com.example.ecommerce.security;

import com.example.ecommerce.model.Role;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads accounts by username or e-mail for Spring Security and turns them into the
 * {@link UserPrincipal}.
 */
@Service
@RequiredArgsConstructor
public class UserAccountDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPrincipal loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(usernameOrEmail)
                .map(UserAccountDetailsService::toPrincipal)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public static UserPrincipal toPrincipal(User user) {
        return new UserPrincipal(user.getId(), user.getUsername(), user.getPassword(), user.isEnabled(),
                user.getTokenVersion(), user.getRoles().stream().map(Role::getName).toList());
    }
}
