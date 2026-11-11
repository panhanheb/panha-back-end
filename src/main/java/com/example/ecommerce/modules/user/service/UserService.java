package com.example.ecommerce.modules.user.service;

import com.example.ecommerce.common.response.PageResponse;
import com.example.ecommerce.modules.user.dto.AdminResetPasswordRequest;
import com.example.ecommerce.modules.user.dto.UpdateUserRolesRequest;
import com.example.ecommerce.modules.user.dto.UpdateUserStatusRequest;
import com.example.ecommerce.modules.user.dto.UserResponse;
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
