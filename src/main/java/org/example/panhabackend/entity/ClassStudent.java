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
 * Enrollment of a student in a class. History is preserved when a student moves to another class.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "class_students",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_student", columnNames = {"class_id", "student_id"}),
        indexes = @Index(name = "idx_class_student_active", columnList = "class_id, active"))
public class ClassStudent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "enrolled_date", nullable = false)
    private LocalDate enrolledDate;

    @Column(name = "left_date")
    private LocalDate leftDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
