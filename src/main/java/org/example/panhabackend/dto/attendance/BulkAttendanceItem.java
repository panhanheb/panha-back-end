package org.example.panhabackend.dto.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.example.panhabackend.enums.AttendanceStatus;

import java.time.LocalTime;

public record BulkAttendanceItem(@NotNull Long studentId,
                                 @NotNull AttendanceStatus status,
                                 LocalTime checkInTime,
                                 @Size(max = 500) String remarks) {
}
