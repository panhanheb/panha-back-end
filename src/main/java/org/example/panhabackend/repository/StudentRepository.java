package org.example.panhabackend.repository;

import org.example.panhabackend.entity.Student;
import org.example.panhabackend.enums.StudentStatus;
import org.example.panhabackend.repository.projection.ClassCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    @Override
    @EntityGraph(attributePaths = {"currentClass"})
    Page<Student> findAll(Specification<Student> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"currentClass"})
    Optional<Student> findWithClassById(Long id);

    @Query("select s from Student s left join fetch s.currentClass where s.id in :ids")
    List<Student> findAllWithClassByIdIn(@Param("ids") Collection<Long> ids);

    Optional<Student> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByStudentCodeIgnoreCase(String studentCode);

    boolean existsByStudentCodeIgnoreCaseAndIdNot(String studentCode, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatus(StudentStatus status);

    long countByCurrentClassIdAndStatus(Long classId, StudentStatus status);

    long countByCurrentClassIdInAndStatus(Collection<Long> classIds, StudentStatus status);

    List<Student> findByCurrentClassIdAndStatusOrderByLastNameAscFirstNameAsc(Long classId, StudentStatus status);

    Page<Student> findByCurrentClassId(Long classId, Pageable pageable);

    @Query("""
            select s.currentClass.id as classId, count(s) as total
            from Student s
            where s.currentClass.id in :classIds and s.status = :status
            group by s.currentClass.id
            """)
    List<ClassCount> countByClassIds(@Param("classIds") Collection<Long> classIds,
                                     @Param("status") StudentStatus status);

    @Query("select distinct s.currentClass.id from Student s where s.status = :status and s.currentClass is not null")
    List<Long> findClassIdsWithStudents(@Param("status") StudentStatus status);

    @Query("select s.id from Student s where s.currentClass.id = :classId and s.status = :status")
    List<Long> findIdsByClassIdAndStatus(@Param("classId") Long classId, @Param("status") StudentStatus status);

    @Modifying
    @Query("update Student s set s.currentClass = null where s.currentClass.id = :classId")
    int clearClass(@Param("classId") Long classId);
}
