package org.example.panhabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.panhabackend.enums.AttendanceStatus;

import java.time.LocalTime;

/**
 * Attendance of one student in one session. The (session_id, student_id) unique constraint
 * prevents duplicate attendance for the same student, class and session.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_records",
        uniqueConstraints = @UniqueConstraint(name = "uk_record_session_student", columnNames = {"session_id", "student_id"}),
        indexes = {
                @Index(name = "idx_record_student_status", columnList = "student_id, status"),
                @Index(name = "idx_record_status", columnList = "status")
        })
public class AttendanceRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AttendanceSession session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttendanceStatus status;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "remarks", length = 500)
    private String remarks;

    /** Teacher who recorded this entry (null when recorded by an administrator). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_teacher_id")
    private Teacher recordedByTeacher;

    /** User account that recorded or last modified this entry. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recorded_by_user_id", nullable = false)
    private User recordedBy;
}
