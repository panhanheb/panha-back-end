package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.report.AttendanceReportResponse;
import org.example.panhabackend.dto.report.AttendanceStats;
import org.example.panhabackend.dto.report.ExportedFile;
import org.example.panhabackend.dto.report.ReportQuery;
import org.example.panhabackend.dto.report.ReportTable;
import org.example.panhabackend.dto.report.StudentAttendanceReportResponse;
import org.example.panhabackend.dto.report.StudentAttendanceSummary;
import org.example.panhabackend.entity.AttendanceRecord;
import org.example.panhabackend.entity.Semester;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.enums.AttendanceStatus;
import org.example.panhabackend.enums.ExportFormat;
import org.example.panhabackend.enums.ReportPeriod;
import org.example.panhabackend.enums.ReportType;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ForbiddenException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.AttendanceMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.SemesterRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.projection.StatusCount;
import org.example.panhabackend.repository.projection.StudentStatusCount;
import org.example.panhabackend.repository.spec.AttendanceRecordSpecifications;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.ExportService;
import org.example.panhabackend.service.ReportService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private static final long MAX_RANGE_DAYS = 366;
    private static final String EXCEL_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final Sort HISTORY_SORT = Sort.by(Sort.Order.desc("session.sessionDate"),
            Sort.Order.asc("session.period"), Sort.Order.asc("id"));

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassRepository classRepository;
    private final SemesterRepository semesterRepository;
    private final AttendanceMapper attendanceMapper;
    private final AccessControlService accessControl;
    private final ExportService exportService;
    private final AppProperties appProperties;

    /** Resolved inclusive date window of a report. */
    record DateRange(ReportPeriod period, LocalDate from, LocalDate to) {
    }

    // ------------------------------------------------------------------ reports

    @Override
    public AttendanceReportResponse attendanceSummary(ReportQuery query) {
        assertCanReportOnClass(query.classId());
        return buildSummary(resolveRange(query), query.classId());
    }

    @Override
    public AttendanceReportResponse classSummary(Long classId, LocalDate fromDate, LocalDate toDate) {
        assertCanReportOnClass(classId);
        LocalDate today = LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : today.withDayOfMonth(1);
        LocalDate to = toDate != null ? toDate : today;
        validateRange(from, to);
        return buildSummary(new DateRange(ReportPeriod.CUSTOM, from, to), classId);
    }

    @Override
    public StudentAttendanceReportResponse studentHistory(Long studentId, LocalDate fromDate, LocalDate toDate) {
        Student student = studentRepository.findWithClassById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        accessControl.assertCanViewStudent(studentId);
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }

        AttendanceFilter filter = new AttendanceFilter(null, studentId, null, null, null, fromDate, toDate);
        List<AttendanceResponse> records = recordRepository
                .findAll(AttendanceRecordSpecifications.of(filter, null), HISTORY_SORT).stream()
                .map(attendanceMapper::toResponse)
                .toList();
        AttendanceStats stats = AttendanceStats.of(
                StatusCount.toMap(recordRepository.countByStatus(fromDate, toDate, null, studentId)), lateAsPresent());

        return new StudentAttendanceReportResponse(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getCurrentClass() != null ? student.getCurrentClass().getName() : null,
                fromDate,
                toDate,
                stats,
                records);
    }

    @Override
    public List<AttendanceResponse> statusList(AttendanceStatus status, ReportQuery query) {
        DateRange range = resolveRange(query);
        AttendanceFilter filter = AttendanceRecordSpecifications.statusInRange(status, query.classId(), range.from(), range.to());
        Collection<Long> scope = null;
        if (!accessControl.isAdmin()) {
            if (accessControl.isTeacher()) {
                scope = accessControl.attendanceClassScope();
            } else {
                filter = filter.withStudentId(accessControl.currentStudent().getId());
            }
        }
        List<AttendanceRecord> records = recordRepository.findAll(AttendanceRecordSpecifications.of(filter, scope), HISTORY_SORT);
        return records.stream().map(attendanceMapper::toResponse).toList();
    }

    // ------------------------------------------------------------------ export

    @Override
    public ExportedFile export(ReportType type, ExportFormat format, ReportQuery query) {
        ReportTable table = switch (type) {
            case SUMMARY -> summaryTable(attendanceSummary(query));
            case ABSENT -> statusTable("Absent Students", AttendanceStatus.ABSENT, query);
            case LATE -> statusTable("Late Students", AttendanceStatus.LATE, query);
            case STUDENT_HISTORY -> {
                if (query.studentId() == null) {
                    throw new BadRequestException("studentId is required for STUDENT_HISTORY exports");
                }
                yield historyTable(studentHistory(query.studentId(), query.fromDate(), query.toDate()));
            }
        };

        String baseName = "attendance-" + type.name().toLowerCase().replace('_', '-') + "-" + LocalDate.now();
        return switch (format) {
            case EXCEL -> new ExportedFile(baseName + ".xlsx", EXCEL_CONTENT_TYPE, exportService.toExcel(table));
            case PDF -> new ExportedFile(baseName + ".pdf", PDF_CONTENT_TYPE, exportService.toPdf(table));
        };
    }

    private ReportTable summaryTable(AttendanceReportResponse report) {
        List<String> subtitles = new ArrayList<>();
        subtitles.add("Period: " + report.period() + " (" + report.fromDate() + " to " + report.toDate() + ")");
        subtitles.add("Class: " + (report.className() != null ? report.className() : "All classes"));
        subtitles.add("School days: " + report.totalSchoolDays() + "   Sessions: " + report.totalSessions());
        AttendanceStats o = report.overall();
        subtitles.add("Overall: present " + o.present() + ", absent " + o.absent() + ", late " + o.late()
                + ", excused " + o.excused() + ", attendance " + o.attendancePercentage() + "%");
        subtitles.add("Generated at: " + report.generatedAt().truncatedTo(ChronoUnit.SECONDS));

        List<List<String>> rows = report.students().stream().map(s -> List.of(
                s.studentCode(),
                s.studentName(),
                nullToEmpty(s.className()),
                String.valueOf(s.stats().totalRecords()),
                String.valueOf(s.stats().present()),
                String.valueOf(s.stats().absent()),
                String.valueOf(s.stats().late()),
                String.valueOf(s.stats().excused()),
                String.valueOf(s.stats().attendancePercentage()))).toList();

        return new ReportTable("Attendance Summary Report", subtitles,
                List.of("Student Code", "Student Name", "Class", "Total", "Present", "Absent", "Late", "Excused", "Attendance %"),
                rows);
    }

    private ReportTable statusTable(String title, AttendanceStatus status, ReportQuery query) {
        DateRange range = resolveRange(query);
        List<AttendanceResponse> records = statusList(status, query);
        List<String> subtitles = List.of(
                "Period: " + range.period() + " (" + range.from() + " to " + range.to() + ")",
                "Records: " + records.size());
        return new ReportTable(title, subtitles,
                List.of("Date", "Period", "Class", "Subject", "Student Code", "Student Name", "Check-in", "Remarks", "Recorded By"),
                records.stream().map(r -> List.of(
                        String.valueOf(r.sessionDate()),
                        String.valueOf(r.period()),
                        nullToEmpty(r.className()),
                        nullToEmpty(r.subjectName()),
                        r.studentCode(),
                        r.studentName(),
                        r.checkInTime() != null ? r.checkInTime().toString() : "",
                        nullToEmpty(r.remarks()),
                        recordedBy(r))).toList());
    }

    private ReportTable historyTable(StudentAttendanceReportResponse report) {
        AttendanceStats s = report.stats();
        List<String> subtitles = List.of(
                "Student: " + report.studentName() + " (" + report.studentCode() + ")",
                "Class: " + nullToEmpty(report.className()),
                "Range: " + (report.fromDate() != null ? report.fromDate() : "beginning") + " to "
                        + (report.toDate() != null ? report.toDate() : "today"),
                "Present " + s.present() + ", absent " + s.absent() + ", late " + s.late() + ", excused " + s.excused()
                        + " - attendance " + s.attendancePercentage() + "%");
        return new ReportTable("Student Attendance History", subtitles,
                List.of("Date", "Period", "Class", "Subject", "Status", "Check-in", "Remarks", "Recorded By"),
                report.records().stream().map(r -> List.of(
                        String.valueOf(r.sessionDate()),
                        String.valueOf(r.period()),
                        nullToEmpty(r.className()),
                        nullToEmpty(r.subjectName()),
                        r.status().name(),
                        r.checkInTime() != null ? r.checkInTime().toString() : "",
                        nullToEmpty(r.remarks()),
                        recordedBy(r))).toList());
    }

    // ------------------------------------------------------------------ helpers

    private AttendanceReportResponse buildSummary(DateRange range, Long classId) {
        String className = null;
        if (classId != null) {
            className = classRepository.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", classId))
                    .getName();
        }
        LocalDate from = range.from();
        LocalDate to = range.to();
        boolean lateAsPresent = lateAsPresent();

        long schoolDays = sessionRepository.countSchoolDays(from, to, classId);
        long sessions = sessionRepository.countSessions(from, to, classId);
        AttendanceStats overall = AttendanceStats.of(
                StatusCount.toMap(recordRepository.countByStatus(from, to, classId, null)), lateAsPresent);

        Map<Long, Map<AttendanceStatus, Long>> perStudent = new HashMap<>();
        for (StudentStatusCount c : recordRepository.countByStudentAndStatus(from, to, classId)) {
            perStudent.computeIfAbsent(c.getStudentId(), k -> new EnumMap<>(AttendanceStatus.class))
                    .merge(c.getStatus(), c.getTotal(), Long::sum);
        }
        Set<Long> studentIds = new HashSet<>(perStudent.keySet());
        if (classId != null) {
            // Include active students with no records yet so the class list is complete.
            studentIds.addAll(studentRepository.findIdsByClassIdAndStatus(classId, StudentStatus.ACTIVE));
        }
        List<Student> students = studentIds.isEmpty() ? List.of() : studentRepository.findAllWithClassByIdIn(studentIds);

        List<StudentAttendanceSummary> summaries = students.stream()
                .map(s -> new StudentAttendanceSummary(
                        s.getId(),
                        s.getStudentCode(),
                        s.getFullName(),
                        s.getCurrentClass() != null ? s.getCurrentClass().getName() : null,
                        AttendanceStats.of(perStudent.getOrDefault(s.getId(), Map.of()), lateAsPresent)))
                .sorted(Comparator.comparing(StudentAttendanceSummary::className,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StudentAttendanceSummary::studentName))
                .toList();

        return new AttendanceReportResponse(range.period(), from, to, classId, className, schoolDays, sessions,
                overall, summaries, LocalDateTime.now());
    }

    DateRange resolveRange(ReportQuery query) {
        ReportPeriod period = query.period();
        if (period == null) {
            period = query.fromDate() != null || query.toDate() != null ? ReportPeriod.CUSTOM : ReportPeriod.DAILY;
        }
        LocalDate date = query.date() != null ? query.date() : LocalDate.now();
        DateRange range = switch (period) {
            case DAILY -> new DateRange(period, date, date);
            case WEEKLY -> new DateRange(period,
                    date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
                    date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)));
            case MONTHLY -> new DateRange(period,
                    date.with(TemporalAdjusters.firstDayOfMonth()),
                    date.with(TemporalAdjusters.lastDayOfMonth()));
            case SEMESTER -> {
                Semester semester = query.semesterId() != null
                        ? semesterRepository.findById(query.semesterId())
                        .orElseThrow(() -> new ResourceNotFoundException("Semester", query.semesterId()))
                        : semesterRepository.findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date)
                        .orElseThrow(() -> new BadRequestException(
                                "No semester covers " + date + "; pass semesterId explicitly"));
                yield new DateRange(period, semester.getStartDate(), semester.getEndDate());
            }
            case CUSTOM -> {
                if (query.fromDate() == null || query.toDate() == null) {
                    throw new BadRequestException("fromDate and toDate are required for CUSTOM reports");
                }
                yield new DateRange(period, query.fromDate(), query.toDate());
            }
        };
        validateRange(range.from(), range.to());
        return range;
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new BadRequestException("Report range cannot exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    /** Admins may report on anything; teachers only on their own classes; students use their own history. */
    private void assertCanReportOnClass(Long classId) {
        if (accessControl.isAdmin()) {
            return;
        }
        if (!accessControl.isTeacher()) {
            throw new ForbiddenException("Students can only view their own attendance history");
        }
        if (classId == null) {
            throw new BadRequestException("classId is required for teacher reports");
        }
        accessControl.assertCanManageClass(classId);
    }

    private boolean lateAsPresent() {
        return appProperties.attendance().countLateAsPresent();
    }

    private static String recordedBy(AttendanceResponse r) {
        return r.recordedByTeacherName() != null ? r.recordedByTeacherName() : nullToEmpty(r.recordedByUsername());
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
