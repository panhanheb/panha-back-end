package org.example.panhabackend.dto.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceSessionResponse(Long id,
                                        Long classId,
                                        String className,
                                        Long subjectId,
                                        String subjectName,
                                        Long semesterId,
                                        String semesterName,
                                        Long teacherId,
                                        String teacherName,
                                        LocalDate sessionDate,
                                        Integer period,
                                        String notes,
                                        long recordCount,
                                        LocalDateTime createdAt) {
}
