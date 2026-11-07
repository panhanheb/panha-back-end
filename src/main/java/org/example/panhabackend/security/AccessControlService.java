package org.example.panhabackend.security;

import lombok.RequiredArgsConstructor;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.enums.RoleName;
import org.example.panhabackend.exception.ForbiddenException;
import org.example.panhabackend.exception.ResourceNotFoundException;
import org.example.panhabackend.repository.SchoolClassRepository;
import org.example.panhabackend.repository.StudentRepository;
import org.example.panhabackend.repository.TeacherClassRepository;
import org.example.panhabackend.repository.TeacherRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Row-level authorization rules that role checks alone cannot express:
 * <ul>
 *     <li>Admins can access everything.</li>
 *     <li>Teachers can access the classes they are assigned to (teacher_classes) or are homeroom teacher of,
 *     and the students currently in those classes.</li>
 *     <li>Students can only access their own records.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessControlService {

    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final TeacherClassRepository teacherClassRepository;
    private final SchoolClassRepository schoolClassRepository;

    public boolean isAdmin() {
        return SecurityUtils.hasRole(RoleName.ROLE_ADMIN);
    }

    public boolean isTeacher() {
        return SecurityUtils.hasRole(RoleName.ROLE_TEACHER);
    }

    public boolean isStudent() {
        return SecurityUtils.hasRole(RoleName.ROLE_STUDENT);
    }

    public Optional<Teacher> currentTeacherIfAny() {
        return SecurityUtils.currentPrincipal().flatMap(p -> teacherRepository.findByUserId(p.getId()));
    }

    /** The teacher profile linked to the current account; 403 if none. */
    public Teacher currentTeacher() {
        return currentTeacherIfAny().orElseThrow(() ->
                new ForbiddenException("Your account is not linked to a teacher profile"));
    }

    /** The student profile linked to the current account; 403 if none. */
    public Student currentStudent() {
        Long userId = SecurityUtils.requirePrincipal().getId();
        return studentRepository.findByUserId(userId).orElseThrow(() ->
                new ForbiddenException("Your account is not linked to a student profile"));
    }

    /** Classes a teacher may work with: explicit assignments plus homeroom classes. */
    public Set<Long> classIdsForTeacher(Long teacherId) {
        Set<Long> ids = new HashSet<>(teacherClassRepository.findClassIdsByTeacherId(teacherId));
        ids.addAll(schoolClassRepository.findIdsByHomeroomTeacherId(teacherId));
        return ids;
    }

    /**
     * Class scope for attendance queries: {@code null} means unrestricted (admin).
     * Students are scoped separately by student id.
     */
    public Collection<Long> attendanceClassScope() {
        if (isAdmin()) {
            return null;
        }
        if (isTeacher()) {
            return classIdsForTeacher(currentTeacher().getId());
        }
        return Set.of();
    }

    /** Admin, or a teacher assigned to the class. Used for recording and editing attendance. */
    public void assertCanManageClass(Long classId) {
        if (isAdmin()) {
            return;
        }
        if (isTeacher() && classIdsForTeacher(currentTeacher().getId()).contains(classId)) {
            return;
        }
        throw new ForbiddenException("You are not assigned to this class");
    }

    /** Admin, a teacher assigned to the class, or a student who belongs to it. */
    public void assertCanViewClass(Long classId) {
        if (isAdmin()) {
            return;
        }
        if (isTeacher() && classIdsForTeacher(currentTeacher().getId()).contains(classId)) {
            return;
        }
        if (isStudent()) {
            SchoolClass c = currentStudent().getCurrentClass();
            if (c != null && c.getId().equals(classId)) {
                return;
            }
        }
        throw new ForbiddenException("You do not have access to this class");
    }

    /** Admin, the student themself, or a teacher of the student's current class. */
    public void assertCanViewStudent(Long studentId) {
        if (isAdmin()) {
            return;
        }
        if (isStudent() && currentStudent().getId().equals(studentId)) {
            return;
        }
        if (isTeacher()) {
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
            SchoolClass c = student.getCurrentClass();
            if (c != null && classIdsForTeacher(currentTeacher().getId()).contains(c.getId())) {
                return;
            }
        }
        throw new ForbiddenException("You do not have access to this student");
    }
}
