package org.example.panhabackend.service.impl;

import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.dto.attendance.AttendanceRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceItem;
import org.example.panhabackend.dto.attendance.BulkAttendanceRequest;
import org.example.panhabackend.entity.AttendanceSession;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.enums.AttendanceStatus;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ForbiddenException;
import org.example.panhabackend.mapper.AttendanceMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.SemesterRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.SubjectRepository;
import org.example.panhabackend.repository.UserRepository;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceImplTest {

    @Mock AttendanceRecordRepository recordRepository;
    @Mock AttendanceSessionRepository sessionRepository;
    @Mock SchoolClassRepository classRepository;
    @Mock StudentRepository studentRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock SemesterRepository semesterRepository;
    @Mock UserRepository userRepository;
    @Mock AttendanceMapper attendanceMapper;
    @Mock AccessControlService accessControl;
    @Mock AuditService auditService;

    private AttendanceServiceImpl service;
    private SchoolClass schoolClass;
    private Student student;
    private final LocalDate today = LocalDate.now();

    @BeforeEach
    void setUp() {
        AppProperties props = new AppProperties(null, null, new AppProperties.Attendance(true, 7), null, null);
        service = new AttendanceServiceImpl(recordRepository, sessionRepository, classRepository, studentRepository,
                subjectRepository, semesterRepository, userRepository, attendanceMapper, accessControl, auditService, props);

        schoolClass = new SchoolClass();
        schoolClass.setId(10L);
        schoolClass.setCode("G10-A");

        student = new Student();
        student.setId(100L);
        student.setStudentCode("STU-100");
        student.setStatus(StudentStatus.ACTIVE);
        student.setCurrentClass(schoolClass);
    }

    @Test
    void recordRejectsDuplicateForSameSession() {
        AttendanceSession existing = new AttendanceSession();
        existing.setId(5L);
        existing.setSchoolClass(schoolClass);
        existing.setSessionDate(today);
        existing.setPeriod(1);

        when(classRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
        when(sessionRepository.findBySchoolClassIdAndSessionDateAndPeriod(10L, today, 1)).thenReturn(Optional.of(existing));
        when(recordRepository.existsBySessionIdAndStudentId(5L, 100L)).thenReturn(true);

        AttendanceRequest request = new AttendanceRequest(10L, null, today, null, 100L,
                AttendanceStatus.PRESENT, null, null);

        assertThatThrownBy(() -> service.record(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already recorded");
        verify(recordRepository, never()).save(any());
    }

    @Test
    void recordRejectsStudentFromAnotherClass() {
        SchoolClass other = new SchoolClass();
        other.setId(99L);
        student.setCurrentClass(other);

        when(classRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));

        AttendanceRequest request = new AttendanceRequest(10L, null, today, 1, 100L,
                AttendanceStatus.ABSENT, null, null);

        assertThatThrownBy(() -> service.record(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not an active member");
    }

    @Test
    void recordIsForbiddenForUnassignedTeacher() {
        doThrow(new ForbiddenException("You are not assigned to this class"))
                .when(accessControl).assertCanManageClass(10L);

        AttendanceRequest request = new AttendanceRequest(10L, null, today, 1, 100L,
                AttendanceStatus.PRESENT, null, null);

        assertThatThrownBy(() -> service.record(request)).isInstanceOf(ForbiddenException.class);
        verify(recordRepository, never()).save(any());
    }

    @Test
    void bulkRejectsRepeatedStudentInRequest() {
        when(classRepository.findById(10L)).thenReturn(Optional.of(schoolClass));

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, null, today, 1, null, List.of(
                new BulkAttendanceItem(100L, AttendanceStatus.PRESENT, null, null),
                new BulkAttendanceItem(100L, AttendanceStatus.LATE, null, null)));

        assertThatThrownBy(() -> service.recordBulk(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getDetails()).containsExactly("studentId 100"));
    }

    @Test
    void bulkRejectsStudentsAlreadyRecordedInSession() {
        AttendanceSession existing = new AttendanceSession();
        existing.setId(5L);
        existing.setSchoolClass(schoolClass);

        when(classRepository.findById(10L)).thenReturn(Optional.of(schoolClass));
        when(studentRepository.findAllById(any())).thenReturn(List.of(student));
        when(sessionRepository.findBySchoolClassIdAndSessionDateAndPeriod(10L, today, 1)).thenReturn(Optional.of(existing));
        when(recordRepository.findExistingStudentIds(any(), any())).thenReturn(List.of(100L));

        BulkAttendanceRequest request = new BulkAttendanceRequest(10L, null, today, 1, null, List.of(
                new BulkAttendanceItem(100L, AttendanceStatus.PRESENT, null, null)));

        assertThatThrownBy(() -> service.recordBulk(request))
                .isInstanceOf(ConflictException.class)
                .satisfies(ex -> assertThat(((ConflictException) ex).getDetails()).containsExactly("STU-100 (id 100)"));
        verify(recordRepository, never()).saveAll(any());
    }
}
