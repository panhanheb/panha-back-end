package org.example.panhabackend.service;

import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.user.AdminResetPasswordRequest;
import org.example.panhabackend.dto.user.UpdateUserRolesRequest;
import org.example.panhabackend.dto.user.UpdateUserStatusRequest;
import org.example.panhabackend.dto.user.UserResponse;
import org.springframework.data.domain.Pageable;

/**
 * Administration of login accounts.
 */
public interface UserService {

    PageResponse<UserResponse> search(String keyword, Boolean enabled, Pageable pageable);

    UserResponse getById(Long id);

    UserResponse updateStatus(Long id, UpdateUserStatusRequest request);

    UserResponse updateRoles(Long id, UpdateUserRolesRequest request);

    void resetPassword(Long id, AdminResetPasswordRequest request);

    void delete(Long id);
}
