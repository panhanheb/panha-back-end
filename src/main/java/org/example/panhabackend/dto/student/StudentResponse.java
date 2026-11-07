package org.example.panhabackend.dto.student;

import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(Long id,
                              String studentCode,
                              String firstName,
                              String lastName,
                              String fullName,
                              Gender gender,
                              LocalDate dateOfBirth,
                              String phone,
                              String email,
                              String address,
                              Long classId,
                              String className,
                              StudentStatus status,
                              Long userId,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
}
