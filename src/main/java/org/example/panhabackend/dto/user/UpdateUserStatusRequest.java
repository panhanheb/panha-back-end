package org.example.panhabackend.dto.user;

import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull Boolean enabled) {
}
