package com.research.experimentplatform.repository;

import com.research.experimentplatform.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActorSupabaseIdOrderByAccessedAtDesc(String actorSupabaseId);

    List<AuditLog> findByResourceTypeAndResourceIdOrderByAccessedAtDesc(String resourceType, String resourceId);
}
