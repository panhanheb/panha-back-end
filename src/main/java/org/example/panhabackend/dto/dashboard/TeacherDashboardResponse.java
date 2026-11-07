package org.example.panhabackend.dto.dashboard;

import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.report.AttendanceStats;

import java.time.LocalDate;
import java.util.List;

public record TeacherDashboardResponse(Long teacherId,
                                       String teacherName,
                                       LocalDate date,
                                       int assignedClassCount,
                                       List<ClassOverview> assignedClasses,
                                       long totalStudents,
                                       AttendanceStats todaySummary,
                                       List<AttendanceResponse> recentRecords) {
}
