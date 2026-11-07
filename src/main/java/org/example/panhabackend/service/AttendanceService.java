package org.example.panhabackend.service;

import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.dto.attendance.AttendanceRequest;
import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.attendance.AttendanceSessionResponse;
import org.example.panhabackend.dto.attendance.AttendanceUpdateRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceRequest;
import org.example.panhabackend.dto.attendance.BulkAttendanceResponse;
import org.example.panhabackend.dto.common.PageResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    /** Records one student's attendance. Rejects duplicates for the same session with 409. */
    AttendanceResponse record(AttendanceRequest request);

    /** Records a whole class roll call in one transaction (all or nothing). */
    BulkAttendanceResponse recordBulk(BulkAttendanceRequest request);

    AttendanceResponse update(Long id, AttendanceUpdateRequest request);

    void delete(Long id);

    AttendanceResponse getById(Long id);

    /** Search scoped to the caller: admins see all, teachers their classes, students themselves. */
    PageResponse<AttendanceResponse> search(AttendanceFilter filter, Pageable pageable);

    PageResponse<AttendanceResponse> getByStudent(Long studentId, AttendanceFilter filter, Pageable pageable);

    PageResponse<AttendanceResponse> getByClass(Long classId, AttendanceFilter filter, Pageable pageable);

    PageResponse<AttendanceSessionResponse> getSessions(Long classId, LocalDate fromDate, LocalDate toDate,
                                                        Pageable pageable);

    List<AttendanceResponse> getSessionRecords(Long sessionId);
}
