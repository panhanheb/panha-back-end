package org.example.panhabackend.dto.attendance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * Record attendance for an entire class in one request (one roll call).
 */
public record BulkAttendanceRequest(
        @NotNull Long classId,
        Long subjectId,
        @NotNull @PastOrPresent(message = "Attendance cannot be recorded for a future date") LocalDate sessionDate,
        @Min(1) @Max(12) Integer period,
        @Size(max = 500) String notes,
        @NotEmpty @Size(max = 200) List<@Valid @NotNull BulkAttendanceItem> records) {
}
