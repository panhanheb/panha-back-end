package org.example.panhabackend.repository;

import org.example.panhabackend.entity.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    @EntityGraph(attributePaths = {"academicYear", "homeroomTeacher"})
    Optional<SchoolClass> findDetailedById(Long id);

    @EntityGraph(attributePaths = {"academicYear", "homeroomTeacher"})
    @Query("""
            select c from SchoolClass c
            where (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(c.code) like lower(concat('%', :keyword, '%')))
              and (:gradeLevel is null or c.gradeLevel = :gradeLevel)
              and (:academicYearId is null or c.academicYear.id = :academicYearId)
            """)
    Page<SchoolClass> search(@Param("keyword") String keyword,
                             @Param("gradeLevel") Integer gradeLevel,
                             @Param("academicYearId") Long academicYearId,
                             Pageable pageable);

    @Query("select c from SchoolClass c where c.id in :ids order by c.gradeLevel, c.name")
    List<SchoolClass> findAllByIdInOrdered(@Param("ids") Collection<Long> ids);

    @Query("select c.id from SchoolClass c where c.homeroomTeacher.id = :teacherId")
    List<Long> findIdsByHomeroomTeacherId(@Param("teacherId") Long teacherId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsByAcademicYearId(Long academicYearId);

    boolean existsByNameIgnoreCaseAndAcademicYearId(String name, Long academicYearId);

    boolean existsByNameIgnoreCaseAndAcademicYearIdAndIdNot(String name, Long academicYearId, Long id);

    @Modifying
    @Query("update SchoolClass c set c.homeroomTeacher = null where c.homeroomTeacher.id = :teacherId")
    int clearHomeroomTeacher(@Param("teacherId") Long teacherId);
}
