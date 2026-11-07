package org.example.panhabackend.repository;

import org.example.panhabackend.entity.Semester;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {

    @EntityGraph(attributePaths = {"academicYear"})
    List<Semester> findAllByOrderByStartDateDesc();

    @EntityGraph(attributePaths = {"academicYear"})
    List<Semester> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);

    @EntityGraph(attributePaths = {"academicYear"})
    Optional<Semester> findDetailedById(Long id);

    /** The semester whose date range contains the given date. */
    Optional<Semester> findFirstByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date1, LocalDate date2);

    boolean existsByAcademicYearIdAndNameIgnoreCase(Long academicYearId, String name);

    boolean existsByAcademicYearIdAndNameIgnoreCaseAndIdNot(Long academicYearId, String name, Long id);

    boolean existsByAcademicYearId(Long academicYearId);

    @Modifying
    @Query("update Semester s set s.current = false where s.current = true and s.id <> :keepId")
    int clearCurrentExcept(@Param("keepId") Long keepId);
}
