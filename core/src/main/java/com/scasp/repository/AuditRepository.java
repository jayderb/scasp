package com.scasp.repository;

import com.scasp.model.AuditEvent;
import java.util.List;

public interface AuditRepository {
    List<AuditEvent> findRecent(int limit);
    List<AuditEvent> findByEntity(String entityType, String entityId);
}
