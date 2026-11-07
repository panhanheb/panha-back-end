package org.example.panhabackend.dto.academic;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SemesterRequest(
        @NotNull Long academicYearId,
        @NotBlank @Size(max = 50) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Boolean current) {

    @AssertTrue(message = "End date must be after start date")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || endDate.isAfter(startDate);
    }
}
