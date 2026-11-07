package org.example.panhabackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subjects", indexes = @Index(name = "idx_subject_name", columnList = "name"))
public class Subject extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "credits")
    private Integer credits;
}
