package org.example.panhabackend.repository;

import org.example.panhabackend.entity.Teacher;
import org.example.panhabackend.enums.TeacherStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long>, JpaSpecificationExecutor<Teacher> {

    Optional<Teacher> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByTeacherCodeIgnoreCase(String teacherCode);

    boolean existsByTeacherCodeIgnoreCaseAndIdNot(String teacherCode, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    long countByStatus(TeacherStatus status);
}
