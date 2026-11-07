package org.example.panhabackend.dto.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SemesterResponse(Long id,
                               Long academicYearId,
                               String academicYearName,
                               String name,
                               LocalDate startDate,
                               LocalDate endDate,
                               boolean current,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
