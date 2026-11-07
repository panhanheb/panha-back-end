package org.example.panhabackend.repository.spec;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.example.panhabackend.dto.attendance.AttendanceFilter;
import org.example.panhabackend.entity.AttendanceRecord;
import org.example.panhabackend.entity.AttendanceSession;
import org.example.panhabackend.enums.AttendanceStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dynamic search criteria for attendance records.
 */
public final class AttendanceRecordSpecifications {

    private AttendanceRecordSpecifications() {
    }

    /**
     * @param filter          optional filters from the query string
     * @param allowedClassIds when non-null, restricts results to these classes (teacher scope);
     *                        an empty collection yields no results
     */
    public static Specification<AttendanceRecord> of(AttendanceFilter filter, Collection<Long> allowedClassIds) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Path<AttendanceSession> session = root.get("session");

            if (filter != null) {
                if (filter.classId() != null) {
                    predicates.add(cb.equal(session.get("schoolClass").get("id"), filter.classId()));
                }
                if (filter.studentId() != null) {
                    predicates.add(cb.equal(root.get("student").get("id"), filter.studentId()));
                }
                if (filter.subjectId() != null) {
                    predicates.add(cb.equal(session.get("subject").get("id"), filter.subjectId()));
                }
                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }
                Path<LocalDate> date = session.get("sessionDate");
                if (filter.date() != null) {
                    predicates.add(cb.equal(date, filter.date()));
                }
                if (filter.fromDate() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(date, filter.fromDate()));
                }
                if (filter.toDate() != null) {
                    predicates.add(cb.lessThanOrEqualTo(date, filter.toDate()));
                }
            }
            if (allowedClassIds != null) {
                if (allowedClassIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(session.get("schoolClass").get("id").in(allowedClassIds));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    public static AttendanceFilter statusInRange(AttendanceStatus status, Long classId, LocalDate from, LocalDate to) {
        return new AttendanceFilter(classId, null, null, status, null, from, to);
    }
}
