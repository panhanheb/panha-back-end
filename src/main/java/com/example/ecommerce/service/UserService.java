package com.example.ecommerce.service;

import com.example.ecommerce.dto.response.PageResponse;
import com.example.ecommerce.dto.request.AdminResetPasswordRequest;
import com.example.ecommerce.dto.request.UpdateUserRolesRequest;
import com.example.ecommerce.dto.request.UpdateUserStatusRequest;
import com.example.ecommerce.dto.response.UserResponse;
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
