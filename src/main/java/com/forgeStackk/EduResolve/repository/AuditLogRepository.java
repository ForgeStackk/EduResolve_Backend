package com.forgeStackk.EduResolve.repository;

import com.forgeStackk.EduResolve.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    @Query(value = "SELECT * FROM audit_log WHERE (:action = '' OR LOWER(action) LIKE LOWER('%' || :action || '%')) " +
                   "AND (:actorId = 0 OR actor_id = :actorId) ORDER BY created_at DESC",
           countQuery = "SELECT COUNT(*) FROM audit_log WHERE (:action = '' OR LOWER(action) LIKE LOWER('%' || :action || '%')) " +
                        "AND (:actorId = 0 OR actor_id = :actorId)",
           nativeQuery = true)
    Page<AuditLog> search(@Param("action") String action,
                          @Param("actorId") long actorId,
                          Pageable pageable);
}
