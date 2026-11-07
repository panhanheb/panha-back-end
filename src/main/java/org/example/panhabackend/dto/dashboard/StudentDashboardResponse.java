package org.example.panhabackend.dto.dashboard;

import org.example.panhabackend.dto.attendance.AttendanceResponse;

import java.util.List;

public record StudentDashboardResponse(Long studentId,
                                       String studentCode,
                                       String studentName,
                                       String className,
                                       double attendancePercentage,
                                       long totalRecords,
                                       long totalPresent,
                                       long totalAbsent,
                                       long totalLate,
                                       long totalExcused,
                                       List<AttendanceResponse> recentHistory) {
}
