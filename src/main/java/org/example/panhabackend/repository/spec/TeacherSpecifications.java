package org.example.panhabackend.repository.spec;

import jakarta.persistence.criteria.Predicate;
import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.enums.TeacherStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic search criteria for teachers.
 */
public final class TeacherSpecifications {

    private TeacherSpecifications() {
    }

    public static Specification<Teacher> search(String keyword, TeacherStatus status, String specialization) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("teacherCode")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(specialization)) {
                predicates.add(cb.like(cb.lower(root.get("specialization")),
                        "%" + specialization.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
