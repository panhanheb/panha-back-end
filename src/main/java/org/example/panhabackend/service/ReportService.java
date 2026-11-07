package org.example.panhabackend.service;

import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.report.AttendanceReportResponse;
import org.example.panhabackend.dto.report.ExportedFile;
import org.example.panhabackend.dto.report.ReportQuery;
import org.example.panhabackend.dto.report.StudentAttendanceReportResponse;
import org.example.panhabackend.enums.AttendanceStatus;
import org.example.panhabackend.enums.ExportFormat;
import org.example.panhabackend.enums.ReportType;

import java.time.LocalDate;
import java.util.List;

public interface ReportService {

    /** Daily / weekly / monthly / semester / custom summary, optionally for one class. */
    AttendanceReportResponse attendanceSummary(ReportQuery query);

    /** Summary for one class over a date range (defaults to the current month). */
    AttendanceReportResponse classSummary(Long classId, LocalDate fromDate, LocalDate toDate);

    /** Full history and statistics for one student (defaults to all time). */
    StudentAttendanceReportResponse studentHistory(Long studentId, LocalDate fromDate, LocalDate toDate);

    /** Records with the given status (ABSENT or LATE lists) in the resolved period. */
    List<AttendanceResponse> statusList(AttendanceStatus status, ReportQuery query);

    ExportedFile export(ReportType type, ExportFormat format, ReportQuery query);
}
