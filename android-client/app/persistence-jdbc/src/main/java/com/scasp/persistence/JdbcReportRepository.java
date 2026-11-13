package com.scasp.persistence;

import com.scasp.report.*;
import com.scasp.repository.ReportRepository;

import javax.sql.DataSource;
import java.util.List;

/** The join reports from database/reports.sql. */
public class JdbcReportRepository extends JdbcSupport implements ReportRepository {
    public JdbcReportRepository(DataSource ds) { super(ds); }

    @Override public List<CapacityRow> capacityReport() {
        return query("SELECT c.course_code, c.title, c.capacity, c.active, COUNT(DISTINCT r.registration_id) AS enrolled, COUNT(DISTINCT w.queue_id) AS waiting "
                        + "FROM courses c "
                        + "LEFT JOIN registrations r ON r.course_code = c.course_code AND r.status = 'ACTIVE' "
                        + "LEFT JOIN waiting_list w ON w.course_code = c.course_code "
                        + "GROUP BY c.course_code, c.title, c.capacity, c.active ORDER BY c.course_code",
                rs -> new CapacityRow(rs.getString("course_code"), rs.getString("title"), rs.getInt("capacity"),
                        rs.getInt("enrolled"), rs.getInt("waiting"), rs.getBoolean("active")));
    }
    @Override public List<StudentRegistrationRow> studentRegistrationReport() {
        return query("SELECT s.student_id, u.full_name, s.programme, c.course_code, c.title, r.created_at "
                        + "FROM students s JOIN app_user u ON u.user_id = s.student_id "
                        + "JOIN registrations r ON r.student_id = s.student_id AND r.status = 'ACTIVE' "
                        + "JOIN courses c ON c.course_code = r.course_code ORDER BY s.student_id, c.course_code",
                rs -> new StudentRegistrationRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), ts(rs, "created_at")));
    }
    @Override public List<WaitingListRow> waitingListReport() {
        return query("SELECT w.course_code, c.title, ROW_NUMBER() OVER (PARTITION BY w.course_code ORDER BY w.queued_at, w.queue_id) AS position, "
                        + "w.student_id, u.full_name, w.queued_at FROM waiting_list w "
                        + "JOIN courses c ON c.course_code = w.course_code JOIN app_user u ON u.user_id = w.student_id "
                        + "ORDER BY w.course_code, position",
                rs -> new WaitingListRow(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString(4), rs.getString(5), ts(rs, "queued_at")));
    }
    @Override public List<WorkloadRow> requestWorkloadReport() {
        return query("SELECT COALESCE(sr.assigned_to, '(unassigned)') AS assignee, COALESCE(u.full_name, '-') AS assignee_name, sr.status, "
                        + "COUNT(*) AS requests, AVG(sr.priority) AS avg_priority FROM service_request sr "
                        + "LEFT JOIN app_user u ON u.user_id = sr.assigned_to "
                        + "GROUP BY sr.assigned_to, u.full_name, sr.status ORDER BY assignee, sr.status",
                rs -> new WorkloadRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getDouble(5)));
    }
    @Override public List<AuditActorRow> auditByActorReport() {
        return query("SELECT a.actor, COALESCE(u.full_name, a.actor) AS actor_name, COALESCE(u.role, 'SYSTEM') AS role, "
                        + "COUNT(*) AS events, MAX(a.occurred_at) AS last_event_at FROM audit_event a "
                        + "LEFT JOIN app_user u ON u.user_id = a.actor GROUP BY a.actor, u.full_name, u.role ORDER BY events DESC, a.actor",
                rs -> new AuditActorRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getLong(4), ts(rs, "last_event_at")));
    }
}
