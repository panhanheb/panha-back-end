package org.example.panhabackend.dto.teacher;

import jakarta.validation.constraints.NotNull;

/**
 * Assign a teacher to a class, optionally for a specific subject.
 */
public record TeacherAssignmentRequest(@NotNull Long classId, Long subjectId) {
}
