package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.dto.attendance.AttendanceRequest;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.attendance.AttendanceSessionResponse;
import org.example.panhabackend.dto.attendance.AttendanceUpdateRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceItem;
import org.example.panhabackend.dto.attendance.BulkAttendanceRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.report.AttendanceStats;
import org.example.panhabackend.entity.AttendanceRecord;
import org.example.panhabackend.entity.AttendanceSession;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.Subject;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.AttendanceStatus;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ForbiddenException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.AttendanceMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.SemesterRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.SubjectRepository;
import org.example.panhabackend.repository.UserRepository;
import org.example.panhabackend.repository.spec.AttendanceRecordSpecifications;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.security.SecurityUtils;
import org.example.panhabackend.service.AttendanceService;
import org.example.panhabackend.service.AuditService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private static final String ENTITY = "AttendanceRecord";
    private static final int DEFAULT_PERIOD = 1;

    private final AttendanceRecordRepository recordRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final AttendanceMapper attendanceMapper;
    private final AccessControlService accessControl;
    private final AuditService auditService;
    private final AppProperties appProperties;

    // ------------------------------------------------------------------ recording

    @Override
    @Transactional
    public AttendanceResponse record(AttendanceRequest request) {
        accessControl.assertCanManageClass(request.classId());
        SchoolClass schoolClass = findClass(request.classId());
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", request.studentId()));
        ensureEnrolled(student, schoolClass);

        int period = request.period() != null ? request.period() : DEFAULT_PERIOD;
        AttendanceSession session = findOrCreateSession(schoolClass, request.subjectId(), request.sessionDate(), period, null);

        if (session.getId() != null && recordRepository.existsBySessionIdAndStudentId(session.getId(), student.getId())) {
            throw new ConflictException("Attendance for student " + student.getStudentCode() + " is already recorded for "
                    + schoolClass.getCode() + " on " + request.sessionDate() + " (period " + period
                    + "). Use PUT /api/attendance/{id} to change it.");
        }

        AttendanceRecord record = newRecord(session, student, request.status(), request.checkInTime(), request.remarks());
        recordRepository.save(record);

        auditService.log(AuditAction.CREATE, ENTITY, record.getId(),
                "Recorded " + record.getStatus() + " for " + student.getStudentCode() + " in " + schoolClass.getCode()
                        + " on " + session.getSessionDate() + " period " + period);
        return attendanceMapper.toResponse(record);
    }

    @Override
    @Transactional
    public BulkAttendanceResponse recordBulk(BulkAttendanceRequest request) {
        accessControl.assertCanManageClass(request.classId());
        SchoolClass schoolClass = findClass(request.classId());

        List<Long> requestedIds = request.records().stream().map(BulkAttendanceItem::studentId).toList();
        Set<Long> uniqueIds = new LinkedHashSet<>(requestedIds);
        if (uniqueIds.size() != requestedIds.size()) {
            Set<Long> seen = new HashSet<>();
            List<String> duplicates = requestedIds.stream().filter(id -> !seen.add(id))
                    .distinct().map(id -> "studentId " + id).toList();
            throw new ConflictException("The request contains the same student more than once", duplicates);
        }

        Map<Long, Student> students = studentRepository.findAllById(uniqueIds).stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
        List<String> missing = uniqueIds.stream().filter(id -> !students.containsKey(id))
                .map(id -> "studentId " + id).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Students not found: " + String.join(", ", missing));
        }
        List<String> notEnrolled = students.values().stream()
                .filter(s -> !isEnrolledActive(s, schoolClass))
                .map(s -> s.getStudentCode() + " (id " + s.getId() + ")")
                .toList();
        if (!notEnrolled.isEmpty()) {
            throw new BadRequestException("These students are not active members of class " + schoolClass.getCode()
                    + ": " + String.join(", ", notEnrolled));
        }

        int period = request.period() != null ? request.period() : DEFAULT_PERIOD;
        AttendanceSession session = findOrCreateSession(schoolClass, request.subjectId(), request.sessionDate(),
                period, request.notes());

        if (session.getId() != null) {
            List<Long> alreadyRecorded = recordRepository.findExistingStudentIds(session.getId(), uniqueIds);
            if (!alreadyRecorded.isEmpty()) {
                List<String> details = alreadyRecorded.stream()
                        .map(id -> students.get(id).getStudentCode() + " (id " + id + ")")
                        .toList();
                throw new ConflictException("Attendance is already recorded for " + alreadyRecorded.size()
                        + " student(s) in this session. Use PUT /api/attendance/{id} to change them.", details);
            }
        }

        List<AttendanceRecord> records = new ArrayList<>();
        Map<AttendanceStatus, Long> counts = new EnumMap<>(AttendanceStatus.class);
        for (BulkAttendanceItem item : request.records()) {
            Student student = students.get(item.studentId());
            records.add(newRecord(session, student, item.status(), item.checkInTime(), item.remarks()));
            counts.merge(item.status(), 1L, Long::sum);
        }
        recordRepository.saveAll(records);

        long totalInSession = recordRepository.countBySessionId(session.getId());
        List<Long> roster = studentRepository.findIdsByClassIdAndStatus(schoolClass.getId(), StudentStatus.ACTIVE);
        Set<Long> marked = roster.isEmpty()
                ? Set.of()
                : new HashSet<>(recordRepository.findExistingStudentIds(session.getId(), roster));
        List<Long> unmarked = roster.stream().filter(id -> !marked.contains(id)).toList();

        auditService.log(AuditAction.CREATE, "AttendanceSession", session.getId(),
                "Bulk recorded " + records.size() + " attendance record(s) for " + schoolClass.getCode()
                        + " on " + session.getSessionDate() + " period " + period + " " + counts);

        return new BulkAttendanceResponse(
                attendanceMapper.toSessionResponse(session, totalInSession),
                records.size(),
                AttendanceStats.of(counts, appProperties.attendance().countLateAsPresent()),
                unmarked,
                records.stream().map(attendanceMapper::toResponse).toList());
    }

    @Override
    @Transactional
    public AttendanceResponse update(Long id, AttendanceUpdateRequest request) {
        AttendanceRecord record = findRecord(id);
        AttendanceSession session = record.getSession();
        accessControl.assertCanManageClass(session.getSchoolClass().getId());

        if (!accessControl.isAdmin()) {
            int window = appProperties.attendance().teacherEditWindowDays();
            if (session.getSessionDate().isBefore(LocalDate.now().minusDays(window))) {
                throw new ForbiddenException("Attendance older than " + window
                        + " day(s) can only be changed by an administrator");
            }
        }

        AttendanceStatus previous = record.getStatus();
        record.setStatus(request.status());
        record.setCheckInTime(request.status() == AttendanceStatus.ABSENT ? null : request.checkInTime());
        record.setRemarks(request.remarks());

        auditService.log(AuditAction.UPDATE, ENTITY, id,
                "Attendance of " + record.getStudent().getStudentCode() + " on " + session.getSessionDate()
                        + " changed " + previous + " -> " + request.status());
        return attendanceMapper.toResponse(record);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        AttendanceRecord record = findRecord(id);
        recordRepository.delete(record);
        auditService.log(AuditAction.DELETE, ENTITY, id,
                "Deleted attendance of " + record.getStudent().getStudentCode() + " on "
                        + record.getSession().getSessionDate());
    }

    // ------------------------------------------------------------------ queries

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getById(Long id) {
        AttendanceRecord record = findRecord(id);
        if (accessControl.isAdmin()) {
            return attendanceMapper.toResponse(record);
        }
        if (accessControl.isTeacher()
                && accessControl.attendanceClassScope().contains(record.getSession().getSchoolClass().getId())) {
            return attendanceMapper.toResponse(record);
        }
        if (accessControl.isStudent() && accessControl.currentStudent().getId().equals(record.getStudent().getId())) {
            return attendanceMapper.toResponse(record);
        }
        throw new ForbiddenException("You do not have access to this attendance record");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> search(AttendanceFilter filter, Pageable pageable) {
        AttendanceFilter effective = filter != null ? filter : new AttendanceFilter(null, null, null, null, null, null, null);
        Collection<Long> scope;
        if (accessControl.isAdmin()) {
            scope = null;
        } else if (accessControl.isTeacher()) {
            scope = accessControl.attendanceClassScope();
        } else {
            // Students only ever see their own records, whatever filter they send.
            effective = effective.withStudentId(accessControl.currentStudent().getId());
            scope = null;
        }
        return page(effective, scope, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getByStudent(Long studentId, AttendanceFilter filter, Pageable pageable) {
        if (!studentRepository.existsById(studentId)) {
            throw new ResourceNotFoundException("Student", studentId);
        }
        accessControl.assertCanViewStudent(studentId);
        AttendanceFilter base = filter != null ? filter : new AttendanceFilter(null, null, null, null, null, null, null);
        return page(base.withStudentId(studentId), null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> getByClass(Long classId, AttendanceFilter filter, Pageable pageable) {
        findClass(classId);
        if (!accessControl.isAdmin() && !accessControl.isTeacher()) {
            throw new ForbiddenException("Only administrators and teachers can view class attendance");
        }
        accessControl.assertCanViewClass(classId);
        AttendanceFilter base = filter != null ? filter : new AttendanceFilter(null, null, null, null, null, null, null);
        return page(base.withClassId(classId), null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceSessionResponse> getSessions(Long classId, LocalDate fromDate, LocalDate toDate,
                                                               Pageable pageable) {
        Collection<Long> scope = accessControl.isAdmin() ? null : accessControl.attendanceClassScope();
        boolean restrict = scope != null;
        // JPQL "in ()" is invalid on some databases, so pass a sentinel when the scope is empty or unused.
        Collection<Long> allowed = restrict && !scope.isEmpty() ? scope : List.of(-1L);
        return PageResponse.from(
                sessionRepository.search(classId, fromDate, toDate, restrict, allowed, pageable),
                s -> attendanceMapper.toSessionResponse(s, recordRepository.countBySessionId(s.getId())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getSessionRecords(Long sessionId) {
        AttendanceSession session = sessionRepository.findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance session", sessionId));
        if (!accessControl.isAdmin()) {
            accessControl.assertCanManageClass(session.getSchoolClass().getId());
        }
        return recordRepository.findBySessionIdOrderByStudentLastNameAsc(sessionId).stream()
                .map(attendanceMapper::toResponse)
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private PageResponse<AttendanceResponse> page(AttendanceFilter filter, Collection<Long> scope, Pageable pageable) {
        validateRange(filter.fromDate(), filter.toDate());
        Specification<AttendanceRecord> spec = AttendanceRecordSpecifications.of(filter, scope);
        return PageResponse.from(recordRepository.findAll(spec, pageable), attendanceMapper::toResponse);
    }

    private AttendanceSession findOrCreateSession(SchoolClass schoolClass, Long subjectId, LocalDate date,
                                                  int period, String notes) {
        Subject subject = subjectId != null
                ? subjectRepository.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject", subjectId))
                : null;

        return sessionRepository.findBySchoolClassIdAndSessionDateAndPeriod(schoolClass.getId(), date, period)
                .map(existing -> {
                    Long existingSubjectId = existing.getSubject() != null ? existing.getSubject().getId() : null;
                    if (subjectId != null && !Objects.equals(existingSubjectId, subjectId)) {
                        throw new ConflictException("Period " + period + " of " + schoolClass.getCode() + " on " + date
                                + " is already recorded for a different subject");
                    }
                    if (notes != null) {
                        existing.setNotes(notes);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    AttendanceSession session = new AttendanceSession();
                    session.setSchoolClass(schoolClass);
                    session.setSubject(subject);
                    session.setSessionDate(date);
                    session.setPeriod(period);
                    session.setNotes(notes);
                    session.setTeacher(accessControl.currentTeacherIfAny().orElse(null));
                    session.setSemester(semesterRepository
                            .findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date)
                            .orElse(null));
                    return sessionRepository.save(session);
                });
    }

    private AttendanceRecord newRecord(AttendanceSession session, Student student, AttendanceStatus status,
                                       LocalTime checkInTime, String remarks) {
        Long userId = SecurityUtils.requirePrincipal().getId();
        User recorder = userRepository.getReferenceById(userId);
        Teacher teacher = accessControl.currentTeacherIfAny().orElse(null);

        AttendanceRecord record = new AttendanceRecord();
        record.setSession(session);
        record.setStudent(student);
        record.setStatus(status);
        record.setCheckInTime(status == AttendanceStatus.ABSENT ? null : checkInTime);
        record.setRemarks(remarks);
        record.setRecordedBy(recorder);
        record.setRecordedByTeacher(teacher);
        return record;
    }

    private void ensureEnrolled(Student student, SchoolClass schoolClass) {
        if (!isEnrolledActive(student, schoolClass)) {
            throw new BadRequestException("Student " + student.getStudentCode()
                    + " is not an active member of class " + schoolClass.getCode());
        }
    }

    private static boolean isEnrolledActive(Student student, SchoolClass schoolClass) {
        return student.getStatus() == StudentStatus.ACTIVE
                && student.getCurrentClass() != null
                && student.getCurrentClass().getId().equals(schoolClass.getId());
    }

    private static void validateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("fromDate must be on or before toDate");
        }
    }

    private SchoolClass findClass(Long id) {
        return classRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", id));
    }

    private AttendanceRecord findRecord(Long id) {
        return recordRepository.findDetailedById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }
}
