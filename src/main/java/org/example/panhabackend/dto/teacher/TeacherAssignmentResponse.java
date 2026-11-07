package org.example.panhabackend.dto.teacher;

import java.time.LocalDateTime;

public record TeacherAssignmentResponse(Long id,
                                        Long teacherId,
                                        String teacherName,
                                        Long classId,
                                        String className,
                                        Long subjectId,
                                        String subjectName,
                                        LocalDateTime createdAt) {
}
