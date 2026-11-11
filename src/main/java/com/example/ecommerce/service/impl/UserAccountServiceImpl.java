package com.example.ecommerce.service.impl;

import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.BusinessException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ErrorCode;
import com.example.ecommerce.model.enums.RoleName;
import com.example.ecommerce.security.SessionVersionLookup;
import com.example.ecommerce.model.enums.AuditAction;
import com.example.ecommerce.service.AuditService;
import com.example.ecommerce.dto.response.AccountView;
import com.example.ecommerce.model.Role;
import com.example.ecommerce.model.User;
import com.example.ecommerce.repository.RoleRepository;
import com.example.ecommerce.repository.UserRepository;
import com.example.ecommerce.service.UserAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService, SessionVersionLookup {

    private static final String ENTITY = "User";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /**
     * Hash compared against when the login is unknown, so a wrong username takes as long as a wrong
     * password and response times do not reveal which accounts exist. Created on first use.
     */
    private volatile String dummyHash;

    @Override
    @Transactional
    public AccountView registerCustomer(String username, String email, String rawPassword, String fullName,
                                        String phone) {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Username '" + normalizedUsername + "' is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException(ErrorCode.DUPLICATE_RESOURCE, "Email '" + normalizedEmail + "' is already registered");
        }
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER is not seeded"));

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setFullName(StringUtils.hasText(fullName) ? fullName.trim() : null);
        user.setPhone(StringUtils.hasText(phone) ? phone.trim() : null);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(new HashSet<>(Set.of(customerRole)));
        user.setEnabled(true);
        userRepository.save(user);

        auditService.log(AuditAction.CREATE, ENTITY, user.getId(), "Customer '" + normalizedUsername + "' signed up",
                user.getId(), user.getUsername());
        return toView(user);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AccountView authenticate(String login, String rawPassword) {
        Optional<User> found = userRepository.findByUsernameOrEmail(login.trim());
        if (found.isEmpty()) {
            if (dummyHash == null) {
                dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
            }
            passwordEncoder.matches(rawPassword, dummyHash);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username/email or password");
        }
        User user = found.get();
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Invalid username/email or password");
        }
        // checked after the password, so a disabled account is only revealed to someone who knows it
        if (!user.isEnabled()) {
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED, "This account is disabled. Please contact support");
        }
        user.setLastLoginAt(LocalDateTime.now());
        auditService.log(AuditAction.LOGIN, ENTITY, user.getId(), "Signed in", user.getId(), user.getUsername());
        return toView(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountView getActiveAccount(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .map(UserAccountServiceImpl::toView)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "This account is no longer active"));
    }

    @Override
    @Transactional
    public AccountView changePassword(Long userId, String currentPassword, String newPassword) {
        User user = activeUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        auditService.log(AuditAction.PASSWORD_CHANGE, ENTITY, user.getId(), "Password changed; all sessions ended");
        return toView(user);
    }

    @Override
    @Transactional
    public int endAllSessions(Long userId) {
        User user = activeUser(userId);
        user.incrementTokenVersion();
        auditService.log(AuditAction.LOGOUT, ENTITY, user.getId(), "Signed out everywhere");
        return user.getTokenVersion();
    }

    @Override
    @Transactional
    public Optional<AccountView> storePasswordResetToken(String email, String tokenHash, LocalDateTime expiresAt) {
        return userRepository.findByEmailIgnoreCase(email.trim()).filter(User::isEnabled).map(user -> {
            user.setPasswordResetTokenHash(tokenHash);
            user.setPasswordResetExpiresAt(expiresAt);
            auditService.log(AuditAction.PASSWORD_RESET_REQUEST, ENTITY, user.getId(), "Password reset requested",
                    user.getId(), user.getUsername());
            return toView(user);
        });
    }

    @Override
    @Transactional(noRollbackFor = BadRequestException.class)
    public AccountView resetPassword(String tokenHash, String newPassword) {
        User user = userRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        boolean expired = user.getPasswordResetExpiresAt() == null
                || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now());
        user.setPasswordResetTokenHash(null); // single use, also when expired
        user.setPasswordResetExpiresAt(null);
        if (expired || !user.isEnabled()) {
            throw new BadRequestException("Invalid or expired password reset token");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        auditService.log(AuditAction.PASSWORD_RESET, ENTITY, user.getId(), "Password reset with token; all sessions ended",
                user.getId(), user.getUsername());
        return toView(user);
    }

    /** Called for every authenticated request: one indexed primary-key lookup. */
    @Override
    @Transactional(readOnly = true)
    public Optional<Integer> activeSessionVersion(Long userId) {
        return userRepository.findActiveSessionVersion(userId);
    }

    private User activeUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isEnabled)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN, "This account is no longer active"));
    }

    private static AccountView toView(User user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toUnmodifiableSet());
        return new AccountView(user.getId(), user.getUsername(), user.getEmail(), roles, user.getTokenVersion());
    }
}
