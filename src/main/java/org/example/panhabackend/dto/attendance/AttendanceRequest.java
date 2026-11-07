package org.example.panhabackend.dto.attendance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.example.panhabackend.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Record attendance for a single student. The session (class + date + period) is created on demand.
 * {@code period} defaults to 1 (the daily roll call).
 */
public record AttendanceRequest(
        @NotNull Long classId,
        Long subjectId,
        @NotNull @PastOrPresent(message = "Attendance cannot be recorded for a future date") LocalDate sessionDate,
        @Min(1) @Max(12) Integer period,
        @NotNull Long studentId,
        @NotNull AttendanceStatus status,
        LocalTime checkInTime,
        @Size(max = 500) String remarks) {
}
