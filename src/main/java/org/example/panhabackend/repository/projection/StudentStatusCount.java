package org.example.panhabackend.repository.projection;

/**
 * Projection of "count of attendance records grouped by student and status".
 */
public interface StudentStatusCount extends StatusCount {

    Long getStudentId();
}
