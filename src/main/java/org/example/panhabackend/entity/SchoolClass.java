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

/**
 * A class (homeroom group), for example "Grade 10 - A", within one academic year.
 * Named SchoolClass to avoid clashing with java.lang.Class.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "classes",
        uniqueConstraints = @UniqueConstraint(name = "uk_class_name_year", columnNames = {"name", "academic_year_id"}),
        indexes = @Index(name = "idx_class_grade", columnList = "grade_level"))
public class SchoolClass extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(name = "section", length = 10)
    private String section;

    @Column(name = "room", length = 30)
    private String room;

    @Column(name = "capacity")
    private Integer capacity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homeroom_teacher_id")
    private Teacher homeroomTeacher;
}
