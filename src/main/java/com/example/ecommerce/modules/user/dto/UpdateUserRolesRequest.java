package com.example.ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;
import com.example.ecommerce.common.security.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleName> roles) {
}
