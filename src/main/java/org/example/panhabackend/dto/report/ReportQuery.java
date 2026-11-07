package org.example.panhabackend.dto.report;

import org.example.panhabackend.enums.ReportPeriod;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query-string parameters shared by the report endpoints.
 *
 * <ul>
 *     <li>DAILY: the day of {@code date} (default today)</li>
 *     <li>WEEKLY: Monday-Sunday week containing {@code date}</li>
 *     <li>MONTHLY: calendar month containing {@code date}</li>
 *     <li>SEMESTER: {@code semesterId}, or the semester containing {@code date}</li>
 *     <li>CUSTOM: {@code fromDate} to {@code toDate} (inclusive, required)</li>
 * </ul>
 */
public record ReportQuery(ReportPeriod period,
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
                          Long semesterId,
                          Long classId,
                          Long studentId) {
}
