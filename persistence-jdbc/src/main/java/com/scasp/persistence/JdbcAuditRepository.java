package com.scasp.persistence;

import com.scasp.model.AuditEvent;
import com.scasp.repository.AuditRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcAuditRepository extends JdbcSupport implements AuditRepository {
    private static final String SELECT = "SELECT event_id, actor, action, entity_type, entity_id, details, occurred_at FROM audit_event ";
    public JdbcAuditRepository(DataSource ds) { super(ds); }
    private static AuditEvent map(ResultSet rs) throws SQLException {
        return new AuditEvent(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), ts(rs, "occurred_at"));
    }
    @Override public List<AuditEvent> findRecent(int limit) {
        int safe = Math.max(1, Math.min(limit, 500));
        return query(SELECT + "ORDER BY event_id DESC LIMIT ?", JdbcAuditRepository::map, safe);
    }
    @Override public List<AuditEvent> findByEntity(String entityType, String entityId) {
        return query(SELECT + "WHERE entity_type = ? AND entity_id = ? ORDER BY event_id", JdbcAuditRepository::map, entityType, entityId);
    }
}
