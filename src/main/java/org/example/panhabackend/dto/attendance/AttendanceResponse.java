package org.example.panhabackend.dto.attendance;

import org.example.panhabackend.enums.AttendanceStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record AttendanceResponse(Long id,
                                 Long sessionId,
                                 LocalDate sessionDate,
                                 Integer period,
                                 Long classId,
                                 String className,
                                 Long subjectId,
                                 String subjectName,
                                 Long studentId,
                                 String studentCode,
                                 String studentName,
                                 AttendanceStatus status,
                                 LocalTime checkInTime,
                                 String remarks,
                                 Long recordedByTeacherId,
                                 String recordedByTeacherName,
                                 String recordedByUsername,
                                 LocalDateTime createdAt,
                                 LocalDateTime updatedAt) {
}
