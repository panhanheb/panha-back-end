package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.user.AdminResetPasswordRequest;
import org.example.panhabackend.dto.user.UpdateUserRolesRequest;
import org.example.panhabackend.dto.user.UpdateUserStatusRequest;
import org.example.panhabackend.dto.user.UserResponse;
import org.example.panhabackend.entity.Role;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.RoleName;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.UserMapper;
import org.example.panhabackend.repository.RoleRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.example.panhabackend.repository.UserRepository;
import org.example.panhabackend.security.SecurityUtils;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ENTITY = "User";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserResponse> search(String keyword, Boolean enabled, Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return PageResponse.from(userRepository.search(kw, enabled, pageable), this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return toResponse(findUser(id));
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest request) {
        User user = findUser(id);
        if (isCurrentUser(id) && !request.enabled()) {
            throw new BadRequestException("You cannot disable your own account");
        }
        user.setEnabled(request.enabled());
        if (!request.enabled()) {
            user.incrementTokenVersion();
        }
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Account " + (request.enabled() ? "enabled" : "disabled"));
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(Long id, UpdateUserRolesRequest request) {
        User user = findUser(id);
        if (isCurrentUser(id) && !request.roles().contains(RoleName.ROLE_ADMIN)) {
            throw new BadRequestException("You cannot remove your own administrator role");
        }
        Set<Role> roles = new HashSet<>();
        for (RoleName name : request.roles()) {
            roles.add(roleRepository.findByName(name)
                    .orElseThrow(() -> new IllegalStateException("Role " + name + " is not seeded")));
        }
        user.setRoles(roles);
        user.incrementTokenVersion(); // force re-login so new tokens carry the new roles
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Roles changed to " + request.roles());
        return toResponse(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminResetPasswordRequest request) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);
        user.incrementTokenVersion();
        auditService.log(AuditAction.PASSWORD_RESET, ENTITY, id, "Password reset by administrator");
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        if (isCurrentUser(id)) {
            throw new BadRequestException("You cannot delete your own account");
        }
        teacherRepository.findByUserId(id).ifPresent(t -> t.setUser(null));
        studentRepository.findByUserId(id).ifPresent(s -> s.setUser(null));
        userRepository.delete(user);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted user '" + user.getUsername() + "'");
    }

    private UserResponse toResponse(User user) {
        Long teacherId = teacherRepository.findByUserId(user.getId()).map(t -> t.getId()).orElse(null);
        Long studentId = studentRepository.findByUserId(user.getId()).map(s -> s.getId()).orElse(null);
        return userMapper.toResponse(user, teacherId, studentId);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private boolean isCurrentUser(Long id) {
        return SecurityUtils.currentPrincipal().map(p -> p.getId().equals(id)).orElse(false);
    }
}
