package org.example.panhabackend.dto.teacher;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.TeacherStatus;

import java.time.LocalDate;

public record TeacherRequest(
        @NotBlank @Size(max = 30) String teacherCode,
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotNull Gender gender,
        @Past LocalDate dateOfBirth,
        @Pattern(regexp = "^[+0-9 ()-]{6,20}$", message = "Invalid phone number") String phone,
        @NotBlank @Email @Size(max = 120) String email,
        @Size(max = 255) String address,
        @Size(max = 100) String specialization,
        @PastOrPresent LocalDate hireDate,
        TeacherStatus status) {
}
