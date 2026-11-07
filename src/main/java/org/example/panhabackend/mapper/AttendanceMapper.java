package org.example.panhabackend.mapper;

import org.example.panhabackend.dto.attendance.AttendanceResponse;
import org.example.panhabackend.dto.attendance.AttendanceSessionResponse;
import org.example.panhabackend.entity.AttendanceRecord;
import org.example.panhabackend.entity.AttendanceSession;
import org.example.panhabackend.entity.Semester;
import org.example.panhabackend.entity.Subject;
import org.example.panhabackend.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(AttendanceRecord r) {
        AttendanceSession s = r.getSession();
        Subject subject = s.getSubject();
        Teacher teacher = r.getRecordedByTeacher();
        return new AttendanceResponse(
                r.getId(),
                s.getId(),
                s.getSessionDate(),
                s.getPeriod(),
                s.getSchoolClass().getId(),
                s.getSchoolClass().getName(),
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                r.getStudent().getId(),
                r.getStudent().getStudentCode(),
                r.getStudent().getFullName(),
                r.getStatus(),
                r.getCheckInTime(),
                r.getRemarks(),
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getFullName() : null,
                r.getRecordedBy() != null ? r.getRecordedBy().getUsername() : null,
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    public AttendanceSessionResponse toSessionResponse(AttendanceSession s, long recordCount) {
        Subject subject = s.getSubject();
        Semester semester = s.getSemester();
        Teacher teacher = s.getTeacher();
        return new AttendanceSessionResponse(
                s.getId(),
                s.getSchoolClass().getId(),
                s.getSchoolClass().getName(),
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                semester != null ? semester.getId() : null,
                semester != null ? semester.getName() : null,
                teacher != null ? teacher.getId() : null,
                teacher != null ? teacher.getFullName() : null,
                s.getSessionDate(),
                s.getPeriod(),
                s.getNotes(),
                recordCount,
                s.getCreatedAt());
    }
}
