package org.example.panhabackend.repository;

import org.example.panhabackend.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    Optional<AcademicYear> findFirstByCurrentTrue();

    @Modifying
    @Query("update AcademicYear y set y.current = false where y.current = true and y.id <> :keepId")
    int clearCurrentExcept(@Param("keepId") Long keepId);
}
