package org.example.panhabackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.dto.common.PageResponse;
import org.example.panhabackend.dto.teacher.TeacherAssignmentRequest;
import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.dto.teacher.TeacherRequest;
import org.example.panhabackend.dto.teacher.TeacherResponse;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Subject;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.entity.TeacherClass;
import org.example.panhabackend.entity.User;
import org.example.panhabackend.enums.AuditAction;
import org.example.panhabackend.enums.TeacherStatus;
import org.example.panhabackend.exception.BadRequestException;
import org.example.panhabackend.exception.ConflictException;
import org.example.panhabackend.exception.ForbiddenException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.mapper.TeacherMapper;
import org.example.panhabackend.repository.AttendanceRecordRepository;
import org.example.panhabackend.repository.AttendanceSessionRepository;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.SubjectRepository;
import org.example.panhabackend.repository.TeacherClassRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.example.panhabackend.repository.spec.TeacherSpecifications;
import org.example.panhabackend.security.AccessControlService;
import org.example.panhabackend.service.AuditService;
import org.example.panhabackend.service.TeacherService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherServiceImpl implements TeacherService {

    private static final String ENTITY = "Teacher";

    private final TeacherRepository teacherRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;
    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final TeacherMapper teacherMapper;
    private final AccessControlService accessControl;
    private final AuditService auditService;

    @Override
    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        validateUniqueness(request, null);
        Teacher teacher = new Teacher();
        teacherMapper.updateEntity(teacher, request);
        teacherRepository.save(teacher);
        auditService.log(AuditAction.CREATE, ENTITY, teacher.getId(), "Created teacher " + teacher.getTeacherCode());
        return teacherMapper.toResponse(teacher);
    }

    @Override
    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        Teacher teacher = findTeacher(id);
        validateUniqueness(request, id);
        teacherMapper.updateEntity(teacher, request);
        auditService.log(AuditAction.UPDATE, ENTITY, id, "Updated teacher " + teacher.getTeacherCode());
        return teacherMapper.toResponse(teacher);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Teacher teacher = findTeacher(id);
        if (sessionRepository.existsByTeacherId(id) || recordRepository.existsByRecordedByTeacherId(id)) {
            throw new ConflictException("Teacher " + teacher.getTeacherCode()
                    + " has recorded attendance and cannot be deleted. Set the status to INACTIVE instead.");
        }
        teacherClassRepository.deleteByTeacherId(id);
        schoolClassRepository.clearHomeroomTeacher(id);
        User account = teacher.getUser();
        if (account != null) {
            account.setEnabled(false);
            account.incrementTokenVersion();
        }
        teacherRepository.delete(teacher);
        auditService.log(AuditAction.DELETE, ENTITY, id, "Deleted teacher " + teacher.getTeacherCode());
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getById(Long id) {
        return teacherMapper.toResponse(findTeacher(id));
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherResponse getMyProfile() {
        return teacherMapper.toResponse(accessControl.currentTeacher());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TeacherResponse> search(String keyword, TeacherStatus status, String specialization,
                                                Pageable pageable) {
        Specification<Teacher> spec = TeacherSpecifications.search(keyword, status, specialization);
        return PageResponse.from(teacherRepository.findAll(spec, pageable), teacherMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherAssignmentResponse> getAssignments(Long teacherId) {
        findTeacher(teacherId);
        if (!accessControl.isAdmin() && !accessControl.currentTeacher().getId().equals(teacherId)) {
            throw new ForbiddenException("You can only view your own assignments");
        }
        return teacherClassRepository.findByTeacherIdOrderByIdAsc(teacherId).stream()
                .map(teacherMapper::toAssignmentResponse)
                .toList();
    }

    @Override
    @Transactional
    public TeacherAssignmentResponse addAssignment(Long teacherId, TeacherAssignmentRequest request) {
        Teacher teacher = findTeacher(teacherId);
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BadRequestException("Only ACTIVE teachers can be assigned to classes");
        }
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Class", request.classId()));
        Subject subject = null;
        if (request.subjectId() != null) {
            subject = subjectRepository.findById(request.subjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Subject", request.subjectId()));
        }

        boolean exists = subject == null
                ? teacherClassRepository.existsByTeacherIdAndSchoolClassIdAndSubjectIsNull(teacherId, schoolClass.getId())
                : teacherClassRepository.existsByTeacherIdAndSchoolClassIdAndSubjectId(teacherId, schoolClass.getId(), subject.getId());
        if (exists) {
            throw new ConflictException("Teacher is already assigned to this class"
                    + (subject != null ? " for subject " + subject.getCode() : ""));
        }

        TeacherClass assignment = new TeacherClass();
        assignment.setTeacher(teacher);
        assignment.setSchoolClass(schoolClass);
        assignment.setSubject(subject);
        teacherClassRepository.save(assignment);

        auditService.log(AuditAction.CREATE, "TeacherClass", assignment.getId(),
                "Assigned teacher " + teacher.getTeacherCode() + " to class " + schoolClass.getCode()
                        + (subject != null ? " / subject " + subject.getCode() : ""));
        return teacherMapper.toAssignmentResponse(assignment);
    }

    @Override
    @Transactional
    public void removeAssignment(Long teacherId, Long assignmentId) {
        TeacherClass assignment = teacherClassRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher assignment", assignmentId));
        if (!assignment.getTeacher().getId().equals(teacherId)) {
            throw new ResourceNotFoundException("Teacher assignment", assignmentId);
        }
        teacherClassRepository.delete(assignment);
        auditService.log(AuditAction.DELETE, "TeacherClass", assignmentId,
                "Removed assignment of teacher " + teacherId + " from class " + assignment.getSchoolClass().getId());
    }

    private void validateUniqueness(TeacherRequest request, Long id) {
        String code = request.teacherCode().trim();
        boolean codeTaken = id == null
                ? teacherRepository.existsByTeacherCodeIgnoreCase(code)
                : teacherRepository.existsByTeacherCodeIgnoreCaseAndIdNot(code, id);
        if (codeTaken) {
            throw new ConflictException("Teacher code '" + code + "' already exists");
        }
        String email = request.email().trim();
        boolean emailTaken = id == null
                ? teacherRepository.existsByEmailIgnoreCase(email)
                : teacherRepository.existsByEmailIgnoreCaseAndIdNot(email, id);
        if (emailTaken) {
            throw new ConflictException("Email '" + email + "' is already used by another teacher");
        }
    }

    private Teacher findTeacher(Long id) {
        return teacherRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    }
}
