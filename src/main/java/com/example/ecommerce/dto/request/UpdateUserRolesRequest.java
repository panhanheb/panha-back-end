package com.example.ecommerce.dto.request;

import jakarta.validation.constraints.NotEmpty;
import com.example.ecommerce.model.enums.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleName> roles) {
}
