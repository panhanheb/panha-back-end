package com.example.ecommerce.modules.user.service.impl;

import lombok.RequiredArgsConstructor;
import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.user.dto.AdminResetPasswordRequest;
import com.example.ecommerce.modules.user.dto.UpdateUserRolesRequest;
import com.example.ecommerce.modules.user.dto.UpdateUserStatusRequest;
import com.example.ecommerce.modules.user.dto.UserResponse;
import com.example.ecommerce.modules.user.entity.Role;
import com.example.ecommerce.modules.user.entity.User;
import com.example.ecommerce.modules.audit.service.AuditAction;
import com.example.ecommerce.common.security.RoleName;
import com.example.ecommerce.common.exception.BadRequestException;
import com.example.ecommerce.common.exception.ConflictException;
import com.example.ecommerce.common.exception.ResourceNotFoundException;
import com.example.ecommerce.modules.user.mapper.UserMapper;
import com.example.ecommerce.modules.cart.repository.CartRepository;
import com.example.ecommerce.modules.order.repository.OrderRepository;
import com.example.ecommerce.modules.user.repository.RoleRepository;
import com.example.ecommerce.modules.user.repository.UserRepository;
import com.example.ecommerce.common.security.SecurityUtils;
import com.example.ecommerce.modules.audit.service.AuditService;
import com.example.ecommerce.modules.user.service.UserService;
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
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
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
        if (orderRepository.existsByUserId(id)) {
            throw new ConflictException("User '" + user.getUsername()
                    + "' has placed orders and cannot be deleted; disable the account instead");
        }
        cartRepository.deleteByUserId(id);
        userRepository.delete(user);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted user '" + user.getUsername() + "'");
    }

    private UserResponse toResponse(User user) {
        return userMapper.toResponse(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private boolean isCurrentUser(Long id) {
        return SecurityUtils.currentPrincipal().map(p -> p.getId().equals(id)).orElse(false);
    }
}
