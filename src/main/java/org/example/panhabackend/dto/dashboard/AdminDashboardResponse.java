package org.example.panhabackend.dto.dashboard;

import org.example.panhabackend.dto.report.AttendanceStats;

import java.time.LocalDate;

/**
 * @param today                   counts of today's attendance records (present, absent, late, excused, percentage)
 * @param classesNotMarkedToday   classes with active students but no roll call today
 */
public record AdminDashboardResponse(LocalDate date,
                                     long totalStudents,
                                     long totalTeachers,
                                     long totalClasses,
                                     long totalSubjects,
                                     long todaySessions,
                                     long todayPresent,
                                     long todayAbsent,
                                     long todayLate,
                                     long todayExcused,
                                     double todayAttendancePercentage,
                                     long classesNotMarkedToday,
                                     AttendanceStats today) {
}
