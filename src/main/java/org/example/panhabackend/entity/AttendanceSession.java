package org.example.panhabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * One roll call of a class: a class on a date for a given period (period 1 is the daily roll call).
 * The unique constraint guarantees only one session per class, date and period.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "attendance_sessions",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_class_date_period",
                columnNames = {"class_id", "session_date", "period"}),
        indexes = {
                @Index(name = "idx_session_date", columnList = "session_date"),
                @Index(name = "idx_session_teacher", columnList = "teacher_id")
        })
public class AttendanceSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id")
    private Semester semester;

    /** Teacher who took the roll call (null when an administrator recorded it). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "period", nullable = false)
    private Integer period = 1;

    @Column(name = "notes", length = 500)
    private String notes;
}
