package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.schoolclass.SchoolClassRequest;
import org.example.panhabackend.dto.schoolclass.SchoolClassResponse;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.entity.AcademicYear;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.enums.TeacherStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.SchoolClassMapper;
import org.example.panhabackend.mapper.StudentMapper;
import org.example.panhabackend.mapper.TeacherMapper;
import org.example.panhabackend.repository.AcademicYearRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.ClassStudentRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.TeacherClassRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.example.panhabackend.repository.projection.ClassCount;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.SchoolClassService;
import org.example.panhabackend.service.StudentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolClassServiceImpl implements SchoolClassService {

    private static final String ENTITY = "Class";

    private final SchoolClassRepository classRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ClassStudentRepository classStudentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final SchoolClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final StudentService studentService;
    private final AccessControlService accessControl;
    private final AuditService auditService;

    @Override
    @Transactional
    public SchoolClassResponse create(SchoolClassRequest request) {
        AcademicYear year = resolveAcademicYear(request.academicYearId());
        validateUniqueness(request, year, null);

        SchoolClass schoolClass = new SchoolClass();
        classMapper.updateEntity(schoolClass, request);
        schoolClass.setAcademicYear(year);
        schoolClass.setHomeroomTeacher(resolveHomeroomTeacher(request.homeroomTeacherId()));
        classRepository.save(schoolClass);

        auditService.log(AuditAction.CREATE, ENTITY, schoolClass.getId(), "Created class " + schoolClass.getCode());
        return classMapper.toResponse(schoolClass, 0);
    }

    @Override
    @Transactional
    public SchoolClassResponse update(Long id, SchoolClassRequest request) {
        SchoolClass schoolClass = findClass(id);
        AcademicYear year = request.academicYearId() != null
                ? resolveAcademicYear(request.academicYearId())
                : schoolClass.getAcademicYear();
        validateUniqueness(request, year, id);

        long activeStudents = studentRepository.countByCurrentClassIdAndStatus(id, StudentStatus.ACTIVE);
        if (request.capacity() != null && request.capacity() < activeStudents) {
            throw new BadRequestException("Capacity " + request.capacity()
                    + " is below the current number of active students (" + activeStudents + ")");
        }

        classMapper.updateEntity(schoolClass, request);
        schoolClass.setAcademicYear(year);
        schoolClass.setHomeroomTeacher(resolveHomeroomTeacher(request.homeroomTeacherId()));

        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated class " + schoolClass.getCode());
        return classMapper.toResponse(schoolClass, activeStudents);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SchoolClass schoolClass = findClass(id);
        if (sessionRepository.existsBySchoolClassId(id)) {
            throw new ConflictException("Class " + schoolClass.getCode()
                    + " has attendance sessions and cannot be deleted");
        }
        studentRepository.clearClass(id);
        classStudentRepository.deleteByClassId(id);
        teacherClassRepository.deleteByClassId(id);
        classRepository.delete(schoolClass);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted class " + schoolClass.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolClassResponse getById(Long id) {
        accessControl.assertCanViewClass(id);
        SchoolClass schoolClass = classRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
        return classMapper.toResponse(schoolClass,
                studentRepository.countByCurrentClassIdAndStatus(id, StudentStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SchoolClassResponse> search(String keyword, Integer gradeLevel, Long academicYearId,
                                                    Pageable pageable) {
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Page<SchoolClass> page = classRepository.search(kw, gradeLevel, academicYearId, pageable);
        Map<Long, Long> counts = studentCounts(page.getContent().stream().map(SchoolClass::getId).toList());
        return PageResponse.from(page, c -> classMapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolClassResponse> getMyClasses() {
        Collection<Long> ids;
        if (accessControl.isTeacher()) {
            ids = accessControl.classIdsForTeacher(accessControl.currentTeacher().getId());
        } else if (accessControl.isStudent()) {
            Student student = accessControl.currentStudent();
            ids = student.getCurrentClass() != null ? List.of(student.getCurrentClass().getId()) : List.of();
        } else {
            ids = List.of();
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        List<SchoolClass> classes = classRepository.findAllByIdInOrdered(ids);
        Map<Long, Long> counts = studentCounts(ids);
        return classes.stream()
                .map(c -> classMapper.toResponse(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> getStudents(Long classId, Pageable pageable) {
        findClass(classId);
        accessControl.assertCanViewClass(classId);
        return PageResponse.from(studentRepository.findByCurrentClassId(classId, pageable), studentMapper::toResponse);
    }

    @Override
    @Transactional
    public List<StudentResponse> enrollStudents(Long classId, List<Long> studentIds) {
        findClass(classId);
        return new LinkedHashSet<>(studentIds).stream()
                .map(studentId -> studentService.assignToClass(studentId, classId))
                .toList();
    }

    @Override
    @Transactional
    public void removeStudent(Long classId, Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        if (student.getCurrentClass() == null || !student.getCurrentClass().getId().equals(classId)) {
            throw new BadRequestException("Student " + student.getStudentCode() + " is not in this class");
        }
        studentService.removeFromClass(studentId);
    }

    @Override
    @Transactional
    public SchoolClassResponse assignHomeroomTeacher(Long classId, Long teacherId) {
        SchoolClass schoolClass = findClass(classId);
        Teacher teacher = resolveHomeroomTeacher(teacherId);
        schoolClass.setHomeroomTeacher(teacher);
        auditService.log(AuditAction.UPDATE, ENTITY, classId,
                "Homeroom teacher of " + schoolClass.getCode() + " set to " + teacher.getTeacherCode());
        return classMapper.toResponse(schoolClass,
                studentRepository.countByCurrentClassIdAndStatus(classId, StudentStatus.ACTIVE));
    }

    @Override
    @Transactional
    public SchoolClassResponse removeHomeroomTeacher(Long classId) {
        SchoolClass schoolClass = findClass(classId);
        schoolClass.setHomeroomTeacher(null);
        auditService.log(AuditAction.UPDATE, ENTITY, classId, "Homeroom teacher removed from " + schoolClass.getCode());
        return classMapper.toResponse(schoolClass,
                studentRepository.countByCurrentClassIdAndStatus(classId, StudentStatus.ACTIVE));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getTeachers(Long classId) {
        findClass(classId);
        accessControl.assertCanViewClass(classId);
        return teacherClassRepository.findBySchoolClassIdOrderByIdAsc(classId).stream()
                .map(teacherMapper::toAssignmentResponse)
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private Map<Long, Long> studentCounts(Collection<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return studentRepository.countByClassIds(classIds, StudentStatus.ACTIVE).stream()
                .collect(Collectors.toMap(ClassCount::getClassId, ClassCount::getTotal));
    }

    private AcademicYear resolveAcademicYear(Long academicYearId) {
        if (academicYearId == null) {
            return academicYearRepository.findFirstByCurrentTrue().orElse(null);
        }
        return academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year", academicYearId));
    }

    private Teacher resolveHomeroomTeacher(Long teacherId) {
        if (teacherId == null) {
            return null;
        }
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", teacherId));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE teachers can be homeroom teachers");
        }
        return teacher;
    }

    private void validateUniqueness(SchoolClassRequest request, AcademicYear year, Long id) {
        String code = request.code().trim();
        boolean codeTaken = id == null
                ? classRepository.existsByCodeIgnoreCase(code)
                : classRepository.existsByCodeIgnoreCaseAndIdNot(code, id);
        if (codeTaken) {
            throw new ConflictException("Class code '" + code + "' already exists");
        }
        if (year != null) {
            String name = request.name().trim();
            boolean nameTaken = id == null
                    ? classRepository.existsByNameIgnoreCaseAndAcademicYearId(name, year.getId())
                    : classRepository.existsByNameIgnoreCaseAndAcademicYearIdAndIdNot(name, year.getId(), id);
            if (nameTaken) {
                throw new ConflictException("A class named '" + name + "' already exists in " + year.getName());
            }
        }
    }

    private SchoolClass findClass(Long id) {
        return classRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }
}
