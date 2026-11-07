package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.config.AppProperties;
import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.dashboard.AdminDashboardResponse;
import org.example.panhabackend.dto.dashboard.ClassOverview;
import org.example.panhabackend.dto.dashboard.StudentDashboardResponse;
import org.example.panhabackend.dto.dashboard.TeacherDashboardResponse;
import org.example.panhabackend.dto.report.AttendanceStats;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.enums.TeacherStatus;
import org.example.panhabackend.mapper.AttendanceMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.SubjectRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.example.panhabackend.repository.projection.ClassCount;
import org.example.panhabackend.repository.projection.StatusCount;
import org.example.panhabackend.repository.spec.AttendanceRecordSpecifications;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_LIMIT = 10;
    private static final Pageable RECENT_PAGE = PageRequest.of(0, RECENT_LIMIT,
            Sort.by(Sort.Order.desc("session.sessionDate"), Sort.Order.desc("id")));

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final SchoolClassRepository classRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final AttendanceMapper attendanceMapper;
    private final AccessControlService accessControl;
    private final AppProperties appProperties;

    @Override
    public AdminDashboardResponse adminDashboard() {
        LocalDate today = LocalDate.now();
        AttendanceStats todayStats = AttendanceStats.of(
                StatusCount.toMap(recordRepository.countByStatus(today, today, null, null)), lateAsPresent());

        Set<Long> classesWithStudents = new HashSet<>(studentRepository.findClassIdsWithStudents(StudentStatus.ACTIVE));
        classesWithStudents.removeAll(sessionRepository.findClassIdsWithSessionOn(today));

        return new AdminDashboardResponse(
                today,
                studentRepository.countByStatus(StudentStatus.ACTIVE),
                teacherRepository.countByStatus(TeacherStatus.ACTIVE),
                classRepository.count(),
                subjectRepository.count(),
                sessionRepository.countBySessionDate(today),
                todayStats.present(),
                todayStats.absent(),
                todayStats.late(),
                todayStats.excused(),
                todayStats.attendancePercentage(),
                classesWithStudents.size(),
                todayStats);
    }

    @Override
    public TeacherDashboardResponse teacherDashboard() {
        LocalDate today = LocalDate.now();
        Teacher teacher = accessControl.currentTeacher();
        Set<Long> classIds = accessControl.classIdsForTeacher(teacher.getId());

        List<ClassOverview> classes = List.of();
        long totalStudents = 0;
        AttendanceStats todaySummary = AttendanceStats.empty();
        List<AttendanceResponse> recent = List.of();

        if (!classIds.isEmpty()) {
            Map<Long, Long> counts = studentRepository.countByClassIds(classIds, StudentStatus.ACTIVE).stream()
                    .collect(Collectors.toMap(ClassCount::getClassId, ClassCount::getTotal));
            Set<Long> markedToday = new HashSet<>(sessionRepository.findClassIdsWithSessionOn(today));
            classes = classRepository.findAllByIdInOrdered(classIds).stream()
                    .map(c -> overview(c, counts.getOrDefault(c.getId(), 0L), markedToday.contains(c.getId())))
                    .toList();
            totalStudents = counts.values().stream().mapToLong(Long::longValue).sum();
            todaySummary = AttendanceStats.of(
                    StatusCount.toMap(recordRepository.countByStatusForClasses(today, classIds)), lateAsPresent());
            recent = recentRecords(null, classIds);
        }

        return new TeacherDashboardResponse(teacher.getId(), teacher.getFullName(), today, classes.size(), classes,
                totalStudents, todaySummary, recent);
    }

    @Override
    public StudentDashboardResponse studentDashboard() {
        Student student = accessControl.currentStudent();
        AttendanceStats stats = AttendanceStats.of(
                StatusCount.toMap(recordRepository.countByStatus(null, null, null, student.getId())), lateAsPresent());
        return new StudentDashboardResponse(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getCurrentClass() != null ? student.getCurrentClass().getName() : null,
                stats.attendancePercentage(),
                stats.totalRecords(),
                stats.present(),
                stats.absent(),
                stats.late(),
                stats.excused(),
                recentRecords(student.getId(), null));
    }

    private List<AttendanceResponse> recentRecords(Long studentId, Collection<Long> classScope) {
        AttendanceFilter filter = new AttendanceFilter(null, studentId, null, null, null, null, null);
        return recordRepository.findAll(AttendanceRecordSpecifications.of(filter, classScope), RECENT_PAGE)
                .map(attendanceMapper::toResponse)
                .getContent();
    }

    private static ClassOverview overview(SchoolClass c, long studentCount, boolean takenToday) {
        return new ClassOverview(c.getId(), c.getCode(), c.getName(), c.getGradeLevel(), studentCount, takenToday);
    }

    private boolean lateAsPresent() {
        return appProperties.attendance().countLateAsPresent();
    }
}
