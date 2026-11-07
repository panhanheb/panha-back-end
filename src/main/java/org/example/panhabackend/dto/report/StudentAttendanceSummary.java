package org.example.panhabackend.dto.report;

public record StudentAttendanceSummary(Long studentId,
                                       String studentCode,
                                       String studentName,
                                       String className,
                                       AttendanceStats stats) {
}
