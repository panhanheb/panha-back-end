package org.example.panhabackend.repository;

import org.example.panhabackend.entity.AttendanceRecord;
import org.example.panhabackend.repository.projection.StatusCount;
import org.example.panhabackend.repository.projection.StudentStatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long>,
        JpaSpecificationExecutor<AttendanceRecord> {

    @Override
    @EntityGraph(attributePaths = {"session", "session.schoolClass", "session.subject",
            "student", "recordedByTeacher", "recordedBy"})
    Page<AttendanceRecord> findAll(Specification<AttendanceRecord> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"session", "session.schoolClass", "session.subject",
            "student", "recordedByTeacher", "recordedBy"})
    List<AttendanceRecord> findAll(Specification<AttendanceRecord> spec, Sort sort);

    @EntityGraph(attributePaths = {"session", "session.schoolClass", "session.subject",
            "student", "recordedByTeacher", "recordedBy"})
    Optional<AttendanceRecord> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"session", "session.schoolClass", "session.subject",
            "student", "recordedByTeacher", "recordedBy"})
    List<AttendanceRecord> findBySessionIdOrderByStudentLastNameAsc(Long sessionId);

    boolean existsBySessionIdAndStudentId(Long sessionId, Long studentId);

    @Query("select r.student.id from AttendanceRecord r where r.session.id = :sessionId and r.student.id in :studentIds")
    List<Long> findExistingStudentIds(@Param("sessionId") Long sessionId,
                                      @Param("studentIds") Collection<Long> studentIds);

    long countBySessionId(Long sessionId);

    boolean existsByStudentId(Long studentId);

    boolean existsByRecordedByTeacherId(Long teacherId);

    /** Status counts in a date range, optionally scoped to a class and/or a student. */
    @Query("""
            select r.status as status, count(r) as total
            from AttendanceRecord r join r.session s
            where (:fromDate is null or s.sessionDate >= :fromDate)
              and (:toDate is null or s.sessionDate <= :toDate)
              and (:classId is null or s.schoolClass.id = :classId)
              and (:studentId is null or r.student.id = :studentId)
            group by r.status
            """)
    List<StatusCount> countByStatus(@Param("fromDate") LocalDate fromDate,
                                    @Param("toDate") LocalDate toDate,
                                    @Param("classId") Long classId,
                                    @Param("studentId") Long studentId);

    /** Status counts for a date restricted to a set of classes (teacher dashboard). */
    @Query("""
            select r.status as status, count(r) as total
            from AttendanceRecord r join r.session s
            where s.sessionDate = :date and s.schoolClass.id in :classIds
            group by r.status
            """)
    List<StatusCount> countByStatusForClasses(@Param("date") LocalDate date,
                                              @Param("classIds") Collection<Long> classIds);

    /** Per-student status counts for reports. */
    @Query("""
            select r.student.id as studentId, r.status as status, count(r) as total
            from AttendanceRecord r join r.session s
            where s.sessionDate between :fromDate and :toDate
              and (:classId is null or s.schoolClass.id = :classId)
            group by r.student.id, r.status
            """)
    List<StudentStatusCount> countByStudentAndStatus(@Param("fromDate") LocalDate fromDate,
                                                     @Param("toDate") LocalDate toDate,
                                                     @Param("classId") Long classId);
}
