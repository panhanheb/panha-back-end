package org.example.panhabackend.dto.schoolclass;

import java.time.LocalDateTime;

public record SchoolClassResponse(Long id,
                                  String code,
                                  String name,
                                  Integer gradeLevel,
                                  String section,
                                  String room,
                                  Integer capacity,
                                  Long academicYearId,
                                  String academicYearName,
                                  Long homeroomTeacherId,
                                  String homeroomTeacherName,
                                  long studentCount,
                                  LocalDateTime createdAt,
                                  LocalDateTime updatedAt) {
}
