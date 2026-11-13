package com.scasp.persistence;

import com.scasp.model.ServiceRequest;
import com.scasp.model.ServiceRequestSummary;
import com.scasp.repository.ServiceRequestRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Service requests with generated keys; status changes reuse the domain state machine (ServiceRequest.canTransition). */
public class JdbcServiceRequestRepository extends JdbcSupport implements ServiceRequestRepository {
    private static final String SELECT = "SELECT request_id, student_id, category, description, priority, status, assigned_to, created_at, updated_at FROM service_request ";
    public JdbcServiceRequestRepository(DataSource ds) { super(ds); }

    private static ServiceRequestSummary map(ResultSet rs) throws SQLException {
        return new ServiceRequestSummary(String.valueOf(rs.getLong("request_id")), rs.getString("student_id"), rs.getString("category"),
                rs.getString("description"), rs.getInt("priority"), ServiceRequest.Status.valueOf(rs.getString("status")),
                rs.getString("assigned_to") == null ? "" : rs.getString("assigned_to"), ts(rs, "created_at"), ts(rs, "updated_at"));
    }
    @Override public long create(String actor, String studentId, String category, String description, int priority) {
        // Domain validation first (same rules as the in-memory core); throws IllegalArgumentException.
        new ServiceRequest("validate", studentId, category, description, priority, java.time.LocalDateTime.now());
        return inTransaction(c -> {
            long id = insertReturningKey(c, "INSERT INTO service_request (student_id, category, description, priority, status) VALUES (?, ?, ?, ?, 'OPEN')",
                    studentId, category.trim().toUpperCase(), description.trim(), priority);
            audit(c, actor, "SUBMIT_REQUEST", "SERVICE_REQUEST", String.valueOf(id), category + ",p" + priority);
            return id;
        });
    }
    @Override public Optional<ServiceRequestSummary> findById(long id) {
        return query(SELECT + "WHERE request_id = ?", JdbcServiceRequestRepository::map, id).stream().findFirst();
    }
    @Override public List<ServiceRequestSummary> findByStudent(String studentId) {
        return query(SELECT + "WHERE student_id = ? ORDER BY created_at DESC, request_id DESC", JdbcServiceRequestRepository::map, studentId);
    }
    @Override public List<ServiceRequestSummary> findOpenByPriority() {
        return query(SELECT + "WHERE status IN ('OPEN','ASSIGNED','IN_PROGRESS') ORDER BY priority DESC, created_at, request_id",
                JdbcServiceRequestRepository::map);
    }
    private static ServiceRequest.Status lockStatus(Connection c, long id) throws SQLException {
        List<String> s = query(c, "SELECT status FROM service_request WHERE request_id = ? FOR UPDATE", rs -> rs.getString(1), id);
        if (s.isEmpty()) throw new IllegalArgumentException("Unknown request");
        return ServiceRequest.Status.valueOf(s.get(0));
    }
    @Override public ServiceRequestSummary assign(String actor, long id, String staffId) {
        return inTransaction(c -> {
            ServiceRequest.Status current = lockStatus(c, id);
            if (!ServiceRequest.canTransition(current, ServiceRequest.Status.ASSIGNED))
                throw new IllegalStateException("Invalid transition " + current + " -> ASSIGNED");
            if (query(c, "SELECT 1 FROM app_user WHERE user_id = ? AND role IN ('STAFF','ADMIN') AND active = TRUE", rs -> 1, staffId).isEmpty())
                throw new IllegalArgumentException("Assignee must be an active staff member");
            update(c, "UPDATE service_request SET status = 'ASSIGNED', assigned_to = ? WHERE request_id = ?", staffId, id);
            audit(c, actor, "ASSIGN_REQUEST", "SERVICE_REQUEST", String.valueOf(id), "to=" + staffId);
            return query(c, SELECT + "WHERE request_id = ?", JdbcServiceRequestRepository::map, id).get(0);
        });
    }
    @Override public ServiceRequestSummary updateStatus(String actor, long id, ServiceRequest.Status next) {
        if (next == ServiceRequest.Status.ASSIGNED) throw new IllegalStateException("Use assign to assign a request");
        return inTransaction(c -> {
            ServiceRequest.Status current = lockStatus(c, id);
            if (!ServiceRequest.canTransition(current, next)) throw new IllegalStateException("Invalid transition " + current + " -> " + next);
            update(c, "UPDATE service_request SET status = ? WHERE request_id = ?", next.name(), id);
            audit(c, actor, "REQUEST_STATUS", "SERVICE_REQUEST", String.valueOf(id), current + "->" + next);
            return query(c, SELECT + "WHERE request_id = ?", JdbcServiceRequestRepository::map, id).get(0);
        });
    }
}
