package org.example.panhabackend.repository;

import org.example.panhabackend.entity.ClassStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    Optional<ClassStudent> findBySchoolClassIdAndStudentId(Long classId, Long studentId);

    List<ClassStudent> findByStudentIdAndActiveTrue(Long studentId);

    @Modifying
    @Query("delete from ClassStudent cs where cs.schoolClass.id = :classId")
    int deleteByClassId(@Param("classId") Long classId);

    @Modifying
    @Query("delete from ClassStudent cs where cs.student.id = :studentId")
    int deleteByStudentId(@Param("studentId") Long studentId);
}
