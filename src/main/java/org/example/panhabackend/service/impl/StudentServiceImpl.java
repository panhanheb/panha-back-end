package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.student.StudentRequest;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.entity.ClassStudent;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.StudentMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.ClassStudentRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.spec.StudentSpecifications;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.StudentService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private static final String ENTITY = "Student";

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassStudentRepository classStudentRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final StudentMapper studentMapper;
    private final AccessControlService accessControl;
    private final AuditService auditService;

    @Override
    @Transactional
    public StudentResponse create(StudentRequest request) {
        validateUniqueness(request, null);
        Student student = new Student();
        studentMapper.updateEntity(student, request);
        studentRepository.save(student);
        if (request.classId() != null) {
            enroll(student, findClass(request.classId()));
        }
        auditService.log(AuditAction.CREATE, ENTITY, student.getId(), "Created student " + student.getStudentCode());
        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = findStudent(id);
        validateUniqueness(request, id);
        studentMapper.updateEntity(student, request);

        Long currentClassId = student.getCurrentClass() != null ? student.getCurrentClass().getId() : null;
        if (!Objects.equals(currentClassId, request.classId())) {
            if (request.classId() == null) {
                unenroll(student);
            } else {
                enroll(student, findClass(request.classId()));
            }
        }
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated student " + student.getStudentCode());
        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Student student = findStudent(id);
        if (attendanceRecordRepository.existsByStudentId(id)) {
            throw new ConflictException("Student " + student.getStudentCode()
                    + " has attendance history and cannot be deleted. Set the status to INACTIVE instead.");
        }
        classStudentRepository.deleteByStudentId(id);
        User account = student.getUser();
        if (account != null) {
            // Keep the account for audit purposes but make sure it can no longer log in.
            account.setEnabled(false);
            account.incrementTokenVersion();
        }
        studentRepository.delete(student);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted student " + student.getStudentCode());
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        accessControl.assertCanViewStudent(id);
        return studentMapper.toResponse(studentRepository.findWithClassById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id)));
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getMyProfile() {
        return studentMapper.toResponse(accessControl.currentStudent());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> search(String keyword, Long classId, StudentStatus status, Gender gender,
                                                Pageable pageable) {
        Collection<Long> scope = accessControl.attendanceClassScope(); // null = admin (unrestricted)
        Specification<Student> spec = StudentSpecifications.search(
                StringUtils.hasText(keyword) ? keyword : null, classId, status, gender, scope);
        return PageResponse.from(studentRepository.findAll(spec, pageable), studentMapper::toResponse);
    }

    @Override
    @Transactional
    public StudentResponse assignToClass(Long studentId, Long classId) {
        Student student = findStudent(studentId);
        SchoolClass target = findClass(classId);
        enroll(student, target);
        auditService.log(AuditAction.UPDATE, ENTITY, studentId,
                "Assigned student " + student.getStudentCode() + " to class " + target.getCode());
        return studentMapper.toResponse(student);
    }

    @Override
    @Transactional
    public StudentResponse removeFromClass(Long studentId) {
        Student student = findStudent(studentId);
        if (student.getCurrentClass() == null) {
            throw new BadRequestException("Student is not assigned to any class");
        }
        String classCode = student.getCurrentClass().getCode();
        unenroll(student);
        auditService.log(AuditAction.UPDATE, ENTITY, studentId,
                "Removed student " + student.getStudentCode() + " from class " + classCode);
        return studentMapper.toResponse(student);
    }

    // ------------------------------------------------------------------ enrollment

    private void enroll(Student student, SchoolClass target) {
        SchoolClass current = student.getCurrentClass();
        if (current != null && current.getId().equals(target.getId())) {
            return;
        }
        if (target.getCapacity() != null
                && studentRepository.countByCurrentClassIdAndStatus(target.getId(), StudentStatus.ACTIVE) >= target.getCapacity()) {
            throw new ConflictException("Class " + target.getCode() + " is full (capacity " + target.getCapacity() + ")");
        }
        closeActiveEnrollments(student);

        ClassStudent enrollment = classStudentRepository
                .findBySchoolClassIdAndStudentId(target.getId(), student.getId())
                .orElseGet(() -> {
                    ClassStudent cs = new ClassStudent();
                    cs.setSchoolClass(target);
                    cs.setStudent(student);
                    return cs;
                });
        enrollment.setActive(true);
        enrollment.setEnrolledDate(LocalDate.now());
        enrollment.setLeftDate(null);
        classStudentRepository.save(enrollment);
        student.setCurrentClass(target);
    }

    private void unenroll(Student student) {
        closeActiveEnrollments(student);
        student.setCurrentClass(null);
    }

    private void closeActiveEnrollments(Student student) {
        for (ClassStudent cs : classStudentRepository.findByStudentIdAndActiveTrue(student.getId())) {
            cs.setActive(false);
            cs.setLeftDate(LocalDate.now());
        }
    }

    // ------------------------------------------------------------------ helpers

    private void validateUniqueness(StudentRequest request, Long id) {
        String code = request.studentCode().trim();
        boolean codeTaken = id == null
                ? studentRepository.existsByStudentCodeIgnoreCase(code)
                : studentRepository.existsByStudentCodeIgnoreCaseAndIdNot(code, id);
        if (codeTaken) {
            throw new ConflictException("Student code '" + code + "' already exists");
        }
        if (StringUtils.hasText(request.email())) {
            String email = request.email().trim();
            boolean emailTaken = id == null
                    ? studentRepository.existsByEmailIgnoreCase(email)
                    : studentRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
            if (emailTaken) {
                throw new ConflictException("Email '" + email + "' is already used by another student");
            }
        }
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }

    private SchoolClass findClass(Long id) {
        return schoolClassRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Class", id));
    }
}
