package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.MessageResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.user.AdminResetPasswordRequest;
import org.example.panhabackend.dto.user.UpdateUserRolesRequest;
import org.example.panhabackend.dto.user.UpdateUserStatusRequest;
import org.example.panhabackend.dto.user.UserResponse;
import org.example.panhabackend.service.UserService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "2. Users", description = "Account administration (ADMIN)")
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Search accounts by username/email and enabled flag")
    @GetMapping
    public PageResponse<UserResponse> search(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Boolean enabled,
                                             @ParameterObject @PageableDefault(sort = "id", direction = Sort.Direction.DESC)
                                             Pageable pageable) {
        return userService.search(keyword, enabled, pageable);
    }

    @Operation(summary = "Get an account")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "Enable or disable an account (disabling revokes its tokens)")
    @PatchMapping("/{id}/status")
    public UserResponse updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return userService.updateStatus(id, request);
    }

    @Operation(summary = "Replace the roles of an account")
    @PutMapping("/{id}/roles")
    public UserResponse updateRoles(@PathVariable Long id, @Valid @RequestBody UpdateUserRolesRequest request) {
        return userService.updateRoles(id, request);
    }

    @Operation(summary = "Set a new password for an account")
    @PostMapping("/{id}/reset-password")
    public MessageResponse resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        userService.resetPassword(id, request);
        return new MessageResponse("Password has been reset");
    }

    @Operation(summary = "Delete an account (linked teacher/student profiles are kept and unlinked)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }
}
