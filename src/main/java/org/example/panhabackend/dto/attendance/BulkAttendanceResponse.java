package org.example.panhabackend.dto.attendance;

import org.example.panhabackend.dto.report.AttendanceStats;

import java.util.List;

/**
 * @param unmarkedStudentIds active students of the class that were not included in the request
 */
public record BulkAttendanceResponse(AttendanceSessionResponse session,
                                     int recordedCount,
                                     AttendanceStats summary,
                                     List<Long> unmarkedStudentIds,
                                     List<AttendanceResponse> records) {
}
