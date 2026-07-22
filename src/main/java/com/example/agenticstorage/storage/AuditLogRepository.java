package com.example.agenticstorage.storage;

import com.example.agenticstorage.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByAgentIdOrderByTimestampDesc(String agentId);
    List<AuditLog> findBySandboxOrderByTimestampDesc(String sandbox);
}
