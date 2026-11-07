package org.example.panhabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "students", indexes = {
        @Index(name = "idx_student_name", columnList = "last_name, first_name"),
        @Index(name = "idx_student_status", columnList = "status")
})
public class Student extends BaseEntity {

    @Column(name = "student_code", nullable = false, unique = true, length = 30)
    private String studentCode;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 10)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", unique = true, length = 120)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    /** The class the student currently belongs to. Enrollment history lives in class_students. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private SchoolClass currentClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StudentStatus status = StudentStatus.ACTIVE;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
