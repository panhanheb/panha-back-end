package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.student.StudentRequest;
import org.example.panhabackend.dto.student.StudentResponse;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.enums.StudentStatus;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {

    /** Copies scalar fields from the request. The class relationship is handled by the service. */
    public void updateEntity(Student student, StudentRequest request) {
        student.setStudentCode(request.studentCode().trim());
        student.setFirstName(request.firstName().trim());
        student.setLastName(request.lastName().trim());
        student.setGender(request.gender());
        student.setDateOfBirth(request.dateOfBirth());
        student.setPhone(request.phone());
        student.setEmail(MapperUtils.normalizeEmail(request.email()));
        student.setAddress(request.address());
        student.setStatus(request.status() != null ? request.status() : StudentStatus.ACTIVE);
    }

    public StudentResponse toResponse(Student s) {
        SchoolClass c = s.getCurrentClass();
        return new StudentResponse(
                s.getId(),
                s.getStudentCode(),
                s.getFirstName(),
                s.getLastName(),
                s.getFullName(),
                s.getGender(),
                s.getDateOfBirth(),
                s.getPhone(),
                s.getEmail(),
                s.getAddress(),
                c != null ? c.getId() : null,
                c != null ? c.getName() : null,
                s.getStatus(),
                s.getUser() != null ? s.getUser().getId() : null,
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
