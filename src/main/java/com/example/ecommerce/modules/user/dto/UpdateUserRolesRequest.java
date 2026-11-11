package com.example.ecommerce.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;
import com.example.ecommerce.modules.user.entity.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleName> roles) {
}
