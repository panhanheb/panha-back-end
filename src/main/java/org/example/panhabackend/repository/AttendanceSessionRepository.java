package org.example.panhabackend.repository;

import org.example.panhabackend.entity.AttendanceSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {

    Optional<AttendanceSession> findBySchoolClassIdAndSessionDateAndPeriod(Long classId, LocalDate date, Integer period);

    @EntityGraph(attributePaths = {"schoolClass", "subject", "semester", "teacher"})
    Optional<AttendanceSession> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"schoolClass", "subject", "semester", "teacher"})
    @Query("""
            select s from AttendanceSession s
            where (:classId is null or s.schoolClass.id = :classId)
              and (:fromDate is null or s.sessionDate >= :fromDate)
              and (:toDate is null or s.sessionDate <= :toDate)
              and (:restrictClasses = false or s.schoolClass.id in :allowedClassIds)
            """)
    Page<AttendanceSession> search(@Param("classId") Long classId,
                                   @Param("fromDate") LocalDate fromDate,
                                   @Param("toDate") LocalDate toDate,
                                   @Param("restrictClasses") boolean restrictClasses,
                                   @Param("allowedClassIds") Collection<Long> allowedClassIds,
                                   Pageable pageable);

    boolean existsBySchoolClassId(Long classId);

    boolean existsByTeacherId(Long teacherId);

    boolean existsBySubjectId(Long subjectId);

    boolean existsBySemesterId(Long semesterId);

    long countBySessionDate(LocalDate date);

    @Query("select distinct s.schoolClass.id from AttendanceSession s where s.sessionDate = :date")
    List<Long> findClassIdsWithSessionOn(@Param("date") LocalDate date);

    @Query("""
            select count(distinct s.sessionDate) from AttendanceSession s
            where s.sessionDate between :fromDate and :toDate
              and (:classId is null or s.schoolClass.id = :classId)
            """)
    long countSchoolDays(@Param("fromDate") LocalDate fromDate,
                         @Param("toDate") LocalDate toDate,
                         @Param("classId") Long classId);

    @Query("""
            select count(s) from AttendanceSession s
            where s.sessionDate between :fromDate and :toDate
              and (:classId is null or s.schoolClass.id = :classId)
            """)
    long countSessions(@Param("fromDate") LocalDate fromDate,
                       @Param("toDate") LocalDate toDate,
                       @Param("classId") Long classId);
}
