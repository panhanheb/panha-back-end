package com.example.ecommerce.modules.audit.repository;

import com.example.ecommerce.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select a from AuditLog a
            where (:entityName is null or a.entityName = :entityName)
              and (:entityId is null or a.entityId = :entityId)
              and (:username is null or a.username = :username)
            """)
    Page<AuditLog> search(@Param("entityName") String entityName,
                          @Param("entityId") Long entityId,
                          @Param("username") String username,
                          Pageable pageable);
}
