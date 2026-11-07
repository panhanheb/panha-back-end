package org.example.panhabackend.dto.report;

import org.example.panhabackend.enums.ReportPeriod;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Daily / weekly / monthly / semester / custom attendance summary, optionally scoped to a class.
 *
 * @param totalSchoolDays distinct dates on which attendance was taken in the range
 * @param totalSessions   number of roll calls in the range
 */
public record AttendanceReportResponse(ReportPeriod period,
                                       LocalDate fromDate,
                                       LocalDate toDate,
                                       Long classId,
                                       String className,
                                       long totalSchoolDays,
                                       long totalSessions,
                                       AttendanceStats overall,
                                       List<StudentAttendanceSummary> students,
                                       LocalDateTime generatedAt) {
}
