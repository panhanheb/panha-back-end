package org.example.panhabackend.dto.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;

import java.time.LocalDate;

/**
 * Payload for creating or updating a student. When {@code status} is omitted it defaults to ACTIVE.
 */
public record StudentRequest(
        @NotBlank @Size(max = 30) String studentCode,
        @NotBlank @Size(max = 60) String firstName,
        @NotBlank @Size(max = 60) String lastName,
        @NotNull Gender gender,
        @Past(message = "Date of birth must be in the past") LocalDate dateOfBirth,
        @Pattern(regexp = "^[+0-9 ()-]{6,20}$", message = "Invalid phone number") String phone,
        @Email @Size(max = 120) String email,
        @Size(max = 255) String address,
        Long classId,
        StudentStatus status) {
}
