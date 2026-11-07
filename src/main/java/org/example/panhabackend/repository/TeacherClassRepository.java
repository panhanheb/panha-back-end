package org.example.panhabackend.repository;

import org.example.panhabackend.entity.TeacherClass;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherClassRepository extends JpaRepository<TeacherClass, Long> {

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject"})
    List<TeacherClass> findByTeacherIdOrderByIdAsc(Long teacherId);

    @EntityGraph(attributePaths = {"teacher", "schoolClass", "subject"})
    List<TeacherClass> findBySchoolClassIdOrderByIdAsc(Long classId);

    @Query("select distinct tc.schoolClass.id from TeacherClass tc where tc.teacher.id = :teacherId")
    List<Long> findClassIdsByTeacherId(@Param("teacherId") Long teacherId);

    boolean existsByTeacherIdAndSchoolClassId(Long teacherId, Long classId);

    boolean existsByTeacherIdAndSchoolClassIdAndSubjectId(Long teacherId, Long classId, Long subjectId);

    boolean existsByTeacherIdAndSchoolClassIdAndSubjectIsNull(Long teacherId, Long classId);

    boolean existsBySubjectId(Long subjectId);

    @Modifying
    @Query("delete from TeacherClass tc where tc.schoolClass.id = :classId")
    int deleteByClassId(@Param("classId") Long classId);

    @Modifying
    @Query("delete from TeacherClass tc where tc.teacher.id = :teacherId")
    int deleteByTeacherId(@Param("teacherId") Long teacherId);
}
