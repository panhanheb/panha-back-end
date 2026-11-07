package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.report.AttendanceReportResponse;
import org.example.panhabackend.dto.report.ExportedFile;
import org.example.panhabackend.dto.report.ReportQuery;
import org.example.panhabackend.dto.report.StudentAttendanceReportResponse;
import org.example.panhabackend.enums.AttendanceStatus;
import org.example.panhabackend.enums.ExportFormat;
import org.example.panhabackend.enums.ReportType;
import org.example.panhabackend.service.ReportService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "10. Reports", description = "Attendance reports and Excel / PDF export")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "Daily / weekly / monthly / semester / custom attendance summary (teachers: classId required)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/attendance")
    public AttendanceReportResponse attendance(@ParameterObject ReportQuery query) {
        return reportService.attendanceSummary(query);
    }

    @Operation(summary = "Class attendance summary for a date range (defaults to the current month)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/class/{classId}")
    public AttendanceReportResponse classSummary(
            @PathVariable Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return reportService.classSummary(classId, fromDate, toDate);
    }

    @Operation(summary = "Student attendance history with percentage (defaults to all time)")
    @GetMapping("/student/{studentId}")
    public StudentAttendanceReportResponse studentHistory(
            @PathVariable Long studentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return reportService.studentHistory(studentId, fromDate, toDate);
    }

    @Operation(summary = "Absent student list for the period (default: today)")
    @GetMapping("/absent")
    public List<AttendanceResponse> absent(@ParameterObject ReportQuery query) {
        return reportService.statusList(AttendanceStatus.ABSENT, query);
    }

    @Operation(summary = "Late student list for the period (default: today)")
    @GetMapping("/late")
    public List<AttendanceResponse> late(@ParameterObject ReportQuery query) {
        return reportService.statusList(AttendanceStatus.LATE, query);
    }

    @Operation(summary = "Export a report as Excel (.xlsx) or PDF")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam ReportType type,
                                         @RequestParam(defaultValue = "EXCEL") ExportFormat format,
                                         @ParameterObject ReportQuery query) {
        ExportedFile file = reportService.export(type, format, query);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .contentType(MediaType.parseMediaType(file.contentType()))
                .contentLength(file.content().length)
                .body(file.content());
    }
}
