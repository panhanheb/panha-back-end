package com.example.ecommerce.modules.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.example.ecommerce.common.config.AppProperties;
import com.example.ecommerce.modules.auth.dto.ChangePasswordRequest;
import com.example.ecommerce.modules.auth.dto.ForgotPasswordRequest;
import com.example.ecommerce.modules.auth.dto.RegisterRequest;
import com.example.ecommerce.modules.auth.dto.ResetPasswordRequest;
import com.example.ecommerce.modules.user.dto.UserResponse;
import com.example.ecommerce.modules.user.entity.Role;
import com.example.ecommerce.modules.user.entity.User;
import com.example.ecommerce.modules.audit.entity.AuditAction;
import com.example.ecommerce.modules.user.entity.RoleName;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ConflictException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.modules.user.repository.RoleRepository;
import com.example.ecommerce.modules.user.repository.UserRepository;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.modules.audit.service.AuditService;
import com.example.ecommerce.modules.auth.service.AuthService;
import com.example.ecommerce.modules.auth.service.PasswordResetNotifier;
import com.example.ecommerce.modules.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String ENTITY = "User";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final AuditService auditService;
    private final PasswordResetNotifier passwordResetNotifier;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim();
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '" + username + "' is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email '" + email + "' is already registered");
        }

        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role ROLE_CUSTOMER is not seeded"));

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(StringUtils.hasText(request.fullName()) ? request.fullName().trim() : null);
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone().trim() : null);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(Set.of(customerRole)));
        user.setEnabled(true);
        userRepository.save(user);

        auditService.log(AuditAction.CREATE, ENTITY, user.getId(), "Customer '" + username + "' signed up",
                user.getId(), user.getUsername());
        return userService.getById(user.getId());
    }

    @Override
    @Transactional
    public void recordLogin(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException(ENTITY, userId));
        user.setLastLoginAt(LocalDateTime.now());
        auditService.log(AuditAction.LOGIN, ENTITY, user.getId(), "User signed in", user.getId(), user.getUsername());
    }

    @Override
    @Transactional
    public void recordLogout(Long userId, String username) {
        auditService.log(AuditAction.LOGOUT, ENTITY, userId, "User signed out", userId, username);
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = currentUserEntity();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("New password must be different from the current password");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.incrementTokenVersion();
        auditService.log(AuditAction.PASSWORD_CHANGE, ENTITY, user.getId(), "Password changed");
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Always behave the same whether or not the e-mail exists, to prevent account enumeration.
        userRepository.findByEmailIgnoreCase(request.email().trim()).filter(User::isEnabled).ifPresent(user -> {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            long ttlMinutes = appProperties.passwordReset().tokenExpirationMinutes();

            user.setPasswordResetTokenHash(sha256(token));
            user.setPasswordResetExpiresAt(LocalDateTime.now().plusMinutes(ttlMinutes));

            String link = appProperties.passwordReset().resetUrl() + "?token=" + token;
            passwordResetNotifier.sendResetLink(user, link, ttlMinutes);
            auditService.log(AuditAction.PASSWORD_RESET_REQUEST, ENTITY, user.getId(),
                    "Password reset requested", user.getId(), user.getUsername());
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetTokenHash(sha256(request.token().trim()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));
        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(LocalDateTime.now())) {
            user.setPasswordResetTokenHash(null);
            user.setPasswordResetExpiresAt(null);
            throw new BadRequestException("Invalid or expired password reset token");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.incrementTokenVersion();
        auditService.log(AuditAction.PASSWORD_RESET, ENTITY, user.getId(), "Password reset with token",
                user.getId(), user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return userService.getById(SecurityUtils.requirePrincipal().getId());
    }

    // ------------------------------------------------------------------ helpers

    private User currentUserEntity() {
        Long id = SecurityUtils.requirePrincipal().getId();
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
