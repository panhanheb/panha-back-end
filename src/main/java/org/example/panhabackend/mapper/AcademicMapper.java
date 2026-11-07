package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.academic.AcademicYearResponse;
import org.example.panhabackend.dto.academic.SemesterResponse;
import org.example.panhabackend.entity.AcademicYear;
import org.example.panhabackend.entity.Semester;
import org.springframework.stereotype.Component;

@Component
public class AcademicMapper {

    public AcademicYearResponse toResponse(AcademicYear y) {
        return new AcademicYearResponse(y.getId(), y.getName(), y.getStartDate(), y.getEndDate(), y.isCurrent(),
                y.getCreatedAt(), y.getUpdatedAt());
    }

    public SemesterResponse toResponse(Semester s) {
        return new SemesterResponse(
                s.getId(),
                s.getAcademicYear().getId(),
                s.getAcademicYear().getName(),
                s.getName(),
                s.getStartDate(),
                s.getEndDate(),
                s.isCurrent(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
