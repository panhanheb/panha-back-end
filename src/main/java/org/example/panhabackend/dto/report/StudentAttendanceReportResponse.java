package org.example.panhabackend.dto.report;

import org.example.panhabackend.dto.attendance.AttendanceResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Full attendance history of one student with summary statistics.
 */
public record StudentAttendanceReportResponse(Long studentId,
                                              String studentCode,
                                              String studentName,
                                              String className,
                                              LocalDate fromDate,
                                              LocalDate toDate,
                                              AttendanceStats stats,
                                              List<AttendanceResponse> records) {
}
