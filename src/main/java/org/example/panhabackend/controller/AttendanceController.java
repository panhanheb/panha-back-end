package org.example.panhabackend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.dto.attendance.AttendanceRequest;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.attendance.AttendanceSessionResponse;
import org.example.panhabackend.dto.attendance.AttendanceUpdateRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.report.AttendanceReportResponse;
import org.example.panhabackend.dto.report.ReportQuery;
import org.example.panhabackend.service.AttendanceService;
import org.example.panhabackend.service.ReportService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "9. Attendance", description = "Record, update and query attendance")
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final ReportService reportService;

    @Operation(summary = "Record attendance for one student (ADMIN or a teacher of the class)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AttendanceResponse record(@Valid @RequestBody AttendanceRequest request) {
        return attendanceService.record(request);
    }

    @Operation(summary = "Record attendance for an entire class in one roll call (all or nothing)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkAttendanceResponse recordBulk(@Valid @RequestBody BulkAttendanceRequest request) {
        return attendanceService.recordBulk(request);
    }

    @Operation(summary = "Search attendance. Scoped automatically: admins see all, teachers their classes, students themselves")
    @GetMapping
    public PageResponse<AttendanceResponse> search(@ParameterObject AttendanceFilter filter,
                                                   @ParameterObject
                                                   @PageableDefault(sort = "session.sessionDate", direction = Sort.Direction.DESC)
                                                   Pageable pageable) {
        return attendanceService.search(filter, pageable);
    }

    @Operation(summary = "Attendance history of the logged-in student")
    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/me")
    public PageResponse<AttendanceResponse> mine(@ParameterObject AttendanceFilter filter,
                                                 @ParameterObject
                                                 @PageableDefault(sort = "session.sessionDate", direction = Sort.Direction.DESC)
                                                 Pageable pageable) {
        return attendanceService.search(filter, pageable);
    }

    @Operation(summary = "Get one attendance record")
    @GetMapping("/{id}")
    public AttendanceResponse getById(@PathVariable Long id) {
        return attendanceService.getById(id);
    }

    @Operation(summary = "Update an attendance record. Teachers may only edit within the configured edit window")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @PutMapping("/{id}")
    public AttendanceResponse update(@PathVariable Long id, @Valid @RequestBody AttendanceUpdateRequest request) {
        return attendanceService.update(id, request);
    }

    @Operation(summary = "Delete an attendance record (ADMIN)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        attendanceService.delete(id);
    }

    @Operation(summary = "Attendance of one student (admin, the student, or a teacher of the student's class)")
    @GetMapping("/student/{studentId}")
    public PageResponse<AttendanceResponse> byStudent(@PathVariable Long studentId,
                                                      @ParameterObject AttendanceFilter filter,
                                                      @ParameterObject
                                                      @PageableDefault(sort = "session.sessionDate", direction = Sort.Direction.DESC)
                                                      Pageable pageable) {
        return attendanceService.getByStudent(studentId, filter, pageable);
    }

    @Operation(summary = "Attendance of one class, filterable by date / range / status")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/class/{classId}")
    public PageResponse<AttendanceResponse> byClass(@PathVariable Long classId,
                                                    @ParameterObject AttendanceFilter filter,
                                                    @ParameterObject
                                                    @PageableDefault(size = 50, sort = "session.sessionDate", direction = Sort.Direction.DESC)
                                                    Pageable pageable) {
        return attendanceService.getByClass(classId, filter, pageable);
    }

    @Operation(summary = "List roll-call sessions (class, date, period, subject, teacher)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/sessions")
    public PageResponse<AttendanceSessionResponse> sessions(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @ParameterObject @PageableDefault(sort = "sessionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return attendanceService.getSessions(classId, fromDate, toDate, pageable);
    }

    @Operation(summary = "All records of one roll-call session")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/sessions/{sessionId}/records")
    public List<AttendanceResponse> sessionRecords(@PathVariable Long sessionId) {
        return attendanceService.getSessionRecords(sessionId);
    }

    @Operation(summary = "Attendance summary report (same as GET /api/reports/attendance)")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    @GetMapping("/report")
    public AttendanceReportResponse report(@ParameterObject ReportQuery query) {
        return reportService.attendanceSummary(query);
    }
}
