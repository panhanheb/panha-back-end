package org.example.panhabackend.dto.schoolclass;

import jakarta.validation.constraints.NotNull;

public record HomeroomTeacherRequest(@NotNull Long teacherId) {
}
