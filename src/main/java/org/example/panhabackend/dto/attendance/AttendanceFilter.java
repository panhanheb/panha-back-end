package org.example.panhabackend.dto.attendance;

import org.example.panhabackend.enums.AttendanceStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query-string filters for attendance searches. All fields are optional.
 * {@code date} matches a single day; {@code fromDate}/{@code toDate} form an inclusive range.
 */
public record AttendanceFilter(Long classId,
                               Long studentId,
                               Long subjectId,
                               AttendanceStatus status,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

    public AttendanceFilter withStudentId(Long id) {
        return new AttendanceFilter(classId, id, subjectId, status, date, fromDate, toDate);
    }

    public AttendanceFilter withClassId(Long id) {
        return new AttendanceFilter(id, studentId, subjectId, status, date, fromDate, toDate);
    }
}
