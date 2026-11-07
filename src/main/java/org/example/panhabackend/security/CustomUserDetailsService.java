package org.example.panhabackend.security;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads accounts by username or email for both login and JWT validation.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserPrincipal loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        return userRepository.findByUsernameOrEmail(usernameOrEmail)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
