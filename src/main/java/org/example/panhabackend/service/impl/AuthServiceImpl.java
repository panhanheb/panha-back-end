package org.example.panhabackend.service.impl;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.dto.auth.AuthResponse;
import org.example.panhabackend.dto.auth.ChangePasswordRequest;
import org.example.panhabackend.dto.auth.ForgotPasswordRequest;
import org.example.panhabackend.dto.auth.LoginRequest;
import org.example.panhabackend.dto.auth.RefreshTokenRequest;
import org.example.panhabackend.dto.auth.RegisterRequest;
import org.example.panhabackend.dto.auth.ResetPasswordRequest;
import org.example.panhabackend.dto.user.UserResponse;
import org.example.panhabackend.entity.Role;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.RoleName;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.exception.UnauthorizedException;
import org.example.panhabackend.repository.RoleRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.example.panhabackend.repository.UserRepository;
import org.example.panhabackend.security.JwtService;
import org.example.panhabackend.security.SecurityUtils;
import org.example.panhabackend.security.UserPrincipal;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.AuthService;
import org.example.panhabackend.service.PasswordResetNotifier;
import org.example.panhabackend.service.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final AuditService auditService;
    private final PasswordResetNotifier passwordResetNotifier;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException / DisabledException, translated to 401 by the exception handler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.usernameOrEmail().trim(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, principal.getId()));
        user.setLastLoginAt(LocalDateTime.now());

        auditService.log(AuditAction.LOGIN, ENTITY, user.getId(), "User logged in", user.getId(), user.getUsername());
        return buildAuthResponse(principal);
    }

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

        Set<Role> roles = resolveRoles(request.roles());

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRoles(roles);
        user.setEnabled(true);
        userRepository.save(user);

        if (request.teacherId() != null) {
            linkTeacher(user, request.teacherId(), request.roles());
        }
        if (request.studentId() != null) {
            linkStudent(user, request.studentId(), request.roles());
        }

        auditService.log(AuditAction.CREATE, ENTITY, user.getId(),
                "Registered user '" + username + "' with roles " + request.roles());
        return userService.getById(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshTokenRequest request) {
        JwtService.TokenClaims claims;
        try {
            claims = jwtService.parse(request.refreshToken(), JwtService.TokenType.REFRESH);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        User user = userRepository.findById(claims.userId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));
        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }
        if (user.getTokenVersion() != claims.tokenVersion() || !user.getUsername().equals(claims.username())) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        return buildAuthResponse(new UserPrincipal(user));
    }

    @Override
    @Transactional
    public void logout() {
        User user = currentUserEntity();
        user.incrementTokenVersion();
        auditService.log(AuditAction.LOGOUT, ENTITY, user.getId(), "User logged out (all tokens revoked)");
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

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        return new AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.generateRefreshToken(principal),
                "Bearer",
                jwtService.getAccessTokenTtlSeconds(),
                userService.getById(principal.getId()));
    }

    private User currentUserEntity() {
        Long id = SecurityUtils.requirePrincipal().getId();
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private Set<Role> resolveRoles(Set<RoleName> names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new IllegalStateException("Role " + name + " is not seeded")));
        }
        return roles;
    }

    private void linkTeacher(User user, Long teacherId, Set<RoleName> roles) {
        if (!roles.contains(RoleName.ROLE_TEACHER)) {
            throw new BadRequestException("An account linked to a teacher must have ROLE_TEACHER");
        }
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
        if (teacher.getUser() != null) {
            throw new ConflictException("Teacher " + teacher.getTeacherCode() + " already has an account");
        }
        teacher.setUser(user);
    }

    private void linkStudent(User user, Long studentId, Set<RoleName> roles) {
        if (!roles.contains(RoleName.ROLE_STUDENT)) {
            throw new BadRequestException("An account linked to a student must have ROLE_STUDENT");
        }
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        if (student.getUser() != null) {
            throw new ConflictException("Student " + student.getStudentCode() + " already has an account");
        }
        student.setUser(user);
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
