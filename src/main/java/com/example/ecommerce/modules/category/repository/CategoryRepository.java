package com.example.ecommerce.modules.category.repository;

import com.example.ecommerce.modules.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    Optional<Category> findBySlug(String slug);

    @Query("""
            select c from Category c
            where (:active is null or c.active = :active)
              and (:keyword is null
                   or lower(c.name) like lower(concat('%', :keyword, '%'))
                   or lower(c.slug) like lower(concat('%', :keyword, '%')))
            """)
    Page<Category> search(@Param("keyword") String keyword, @Param("active") Boolean active, Pageable pageable);
}
