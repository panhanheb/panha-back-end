package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.teacher.TeacherAssignmentResponse;
import org.example.panhabackend.dto.teacher.TeacherRequest;
import org.example.panhabackend.dto.teacher.TeacherResponse;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.entity.TeacherClass;
import org.example.panhabackend.enums.TeacherStatus;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {

    public void updateEntity(Teacher teacher, TeacherRequest request) {
        teacher.setTeacherCode(request.teacherCode().trim());
        teacher.setFirstName(request.firstName().trim());
        teacher.setLastName(request.lastName().trim());
        teacher.setGender(request.gender());
        teacher.setDateOfBirth(request.dateOfBirth());
        teacher.setPhone(request.phone());
        teacher.setEmail(MapperUtils.normalizeEmail(request.email()));
        teacher.setAddress(request.address());
        teacher.setSpecialization(request.specialization());
        teacher.setHireDate(request.hireDate());
        teacher.setStatus(request.status() != null ? request.status() : TeacherStatus.ACTIVE);
    }

    public TeacherResponse toResponse(Teacher t) {
        return new TeacherResponse(
                t.getId(),
                t.getTeacherCode(),
                t.getFirstName(),
                t.getLastName(),
                t.getFullName(),
                t.getGender(),
                t.getDateOfBirth(),
                t.getPhone(),
                t.getEmail(),
                t.getAddress(),
                t.getSpecialization(),
                t.getHireDate(),
                t.getStatus(),
                t.getUser() != null ? t.getUser().getId() : null,
                t.getCreatedAt(),
                t.getUpdatedAt());
    }

    public TeacherAssignmentResponse toAssignmentResponse(TeacherClass tc) {
        return new TeacherAssignmentResponse(
                tc.getId(),
                tc.getTeacher().getId(),
                tc.getTeacher().getFullName(),
                tc.getSchoolClass().getId(),
                tc.getSchoolClass().getName(),
                tc.getSubject() != null ? tc.getSubject().getId() : null,
                tc.getSubject() != null ? tc.getSubject().getName() : null,
                tc.getCreatedAt());
    }
}
