package org.example.panhabackend.dto.student;

import jakarta.validation.constraints.NotNull;

public record AssignClassRequest(@NotNull Long classId) {
}
