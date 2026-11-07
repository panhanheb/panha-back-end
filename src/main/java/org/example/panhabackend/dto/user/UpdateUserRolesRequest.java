package org.example.panhabackend.dto.user;

import jakarta.validation.constraints.NotEmpty;
import org.example.panhabackend.enums.RoleName;

import java.util.Set;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleName> roles) {
}
