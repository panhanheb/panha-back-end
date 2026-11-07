package org.example.panhabackend.dto.schoolclass;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EnrollStudentsRequest(@NotEmpty @Size(max = 200) List<@NotNull Long> studentIds) {
}
