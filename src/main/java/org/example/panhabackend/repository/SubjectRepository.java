package org.example.panhabackend.repository;

import org.example.panhabackend.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    @Query("""
            select s from Subject s
            where :keyword is null
               or lower(s.name) like lower(concat('%', :keyword, '%'))
               or lower(s.code) like lower(concat('%', :keyword, '%'))
            """)
    Page<Subject> search(@Param("keyword") String keyword, Pageable pageable);
}
