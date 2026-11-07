package org.example.panhabackend.dto.schoolclass;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SchoolClassRequest(
        @NotBlank @Size(max = 30) String code,
        @NotBlank @Size(max = 100) String name,
        @NotNull @Min(1) @Max(13) Integer gradeLevel,
        @Size(max = 10) String section,
        @Size(max = 30) String room,
        @Min(1) @Max(200) Integer capacity,
        Long academicYearId,
        Long homeroomTeacherId) {
}
