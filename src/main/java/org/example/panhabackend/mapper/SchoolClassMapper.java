package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.schoolclass.SchoolClassRequest;
import org.example.panhabackend.dto.schoolclass.SchoolClassResponse;
import org.example.panhabackend.entity.AcademicYear;
import org.example.panhabackend.entity.SchoolClass;
import org.example.panhabackend.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class SchoolClassMapper {

    /** Copies scalar fields. Academic year and homeroom teacher are resolved by the service. */
    public void updateEntity(SchoolClass c, SchoolClassRequest request) {
        c.setCode(request.code().trim());
        c.setName(request.name().trim());
        c.setGradeLevel(request.gradeLevel());
        c.setSection(request.section());
        c.setRoom(request.room());
        c.setCapacity(request.capacity());
    }

    public SchoolClassResponse toResponse(SchoolClass c, long studentCount) {
        AcademicYear year = c.getAcademicYear();
        Teacher homeroom = c.getHomeroomTeacher();
        return new SchoolClassResponse(
                c.getId(),
                c.getCode(),
                c.getName(),
                c.getGradeLevel(),
                c.getSection(),
                c.getRoom(),
                c.getCapacity(),
                year != null ? year.getId() : null,
                year != null ? year.getName() : null,
                homeroom != null ? homeroom.getId() : null,
                homeroom != null ? homeroom.getFullName() : null,
                studentCount,
                c.getCreatedAt(),
                c.getUpdatedAt());
    }
}
