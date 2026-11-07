package org.example.panhabackend.entity;

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

/**
 * Assignment of a teacher to a class, optionally for a specific subject.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "teacher_classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_teacher_class_subject", columnNames = {"teacher_id", "class_id", "subject_id"}),
        indexes = @Index(name = "idx_teacher_classes_class", columnList = "class_id"))
public class TeacherClass extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private SchoolClass schoolClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;
}
