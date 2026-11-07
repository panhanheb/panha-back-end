package org.example.panhabackend.dto.teacher;

import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.TeacherStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TeacherResponse(Long id,
                              String teacherCode,
                              String firstName,
                              String lastName,
                              String fullName,
                              Gender gender,
                              LocalDate dateOfBirth,
                              String phone,
                              String email,
                              String address,
                              String specialization,
                              LocalDate hireDate,
                              TeacherStatus status,
                              Long userId,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
}
