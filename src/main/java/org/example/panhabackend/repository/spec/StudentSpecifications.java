package org.example.panhabackend.repository.spec;

import jakarta.persistence.criteria.Predicate;
import org.example.panhabackend.entity.Student;
import org.example.panhabackend.enums.Gender;
import org.example.panhabackend.enums.StudentStatus;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dynamic search criteria for students.
 */
public final class StudentSpecifications {

    private StudentSpecifications() {
    }

    /**
     * @param allowedClassIds when non-null, restricts results to students currently in these classes
     */
    public static Specification<Student> search(String keyword, Long classId, StudentStatus status, Gender gender,
                                                Collection<Long> allowedClassIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (allowedClassIds != null) {
                if (allowedClassIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(root.get("currentClass").get("id").in(allowedClassIds));
            }
            if (StringUtils.hasText(keyword)) {
                String like = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("studentCode")), like),
                        cb.like(cb.lower(root.get("firstName")), like),
                        cb.like(cb.lower(root.get("lastName")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(root.get("phone")), like)));
            }
            if (classId != null) {
                predicates.add(cb.equal(root.get("currentClass").get("id"), classId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (gender != null) {
                predicates.add(cb.equal(root.get("gender"), gender));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
