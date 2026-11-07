package org.example.panhabackend.dto.academic;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AcademicYearResponse(Long id,
                                   String name,
                                   LocalDate startDate,
                                   LocalDate endDate,
                                   boolean current,
                                   LocalDateTime createdAt,
                                   LocalDateTime updatedAt) {
}
