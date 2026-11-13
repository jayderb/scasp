package com.scasp.persistence;

import com.scasp.model.Registration;
import com.scasp.model.WaitlistEntry;
import com.scasp.repository.RegistrationOutcome;
import com.scasp.repository.RegistrationRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Atomic registration / withdrawal. Each operation is ONE transaction that touches registrations,
 * waiting_list and audit_event: either every change commits or none does.
 * Concurrency: {@code SELECT ... FOR UPDATE} on the course row serialises all registrations for the same
 * course, so the capacity check-then-insert cannot race (the database equivalent of the Stage 2 fix).
 */
public class JdbcRegistrationRepository extends JdbcSupport implements RegistrationRepository {
    public JdbcRegistrationRepository(DataSource ds) { super(ds); }
    /** @param containerManaged true inside a container-managed (JTA) transaction such as RegistrationBean */
    public JdbcRegistrationRepository(DataSource ds, boolean containerManaged) { super(ds, containerManaged); }

    private record CourseLock(int capacity, boolean active) {}

    private static CourseLock lockCourse(Connection c, String courseCode) throws SQLException {
        List<CourseLock> rows = query(c, "SELECT capacity, active FROM courses WHERE course_code = ? FOR UPDATE",
                rs -> new CourseLock(rs.getInt(1), rs.getBoolean(2)), courseCode);
        if (rows.isEmpty()) throw new IllegalArgumentException("Unknown course");
        return rows.get(0);
    }

    @Override public RegistrationOutcome register(String actor, String studentId, String courseCode) {
        String code = courseCode == null ? null : courseCode.trim().toUpperCase();
        return inTransaction(c -> {
            CourseLock course = lockCourse(c, code);
            if (!course.active()) throw new IllegalStateException("Course is inactive");
            if (query(c, "SELECT 1 FROM students WHERE student_id = ?", rs -> 1, studentId).isEmpty())
                throw new IllegalArgumentException("Unknown student");
            List<String> existing = query(c, "SELECT status FROM registrations WHERE student_id = ? AND course_code = ?",
                    rs -> rs.getString(1), studentId, code);
            if (existing.contains("ACTIVE")) throw new IllegalStateException("Duplicate registration");
            if (!query(c, "SELECT 1 FROM waiting_list WHERE student_id = ? AND course_code = ?", rs -> 1, studentId, code).isEmpty())
                throw new IllegalStateException("Already on waiting list");
            int enrolled = query(c, "SELECT COUNT(*) FROM registrations WHERE course_code = ? AND status = 'ACTIVE'",
                    rs -> rs.getInt(1), code).get(0);
            RegistrationOutcome outcome;
            if (enrolled < course.capacity()) {
                long id;
                if (existing.isEmpty()) {
                    id = insertReturningKey(c, "INSERT INTO registrations (student_id, course_code, status) VALUES (?, ?, 'ACTIVE')", studentId, code);
                } else {   // re-registration after withdrawal re-activates the existing row (unique student+course)
                    update(c, "UPDATE registrations SET status = 'ACTIVE', created_at = CURRENT_TIMESTAMP WHERE student_id = ? AND course_code = ?", studentId, code);
                    id = query(c, "SELECT registration_id FROM registrations WHERE student_id = ? AND course_code = ?", rs -> rs.getLong(1), studentId, code).get(0);
                }
                outcome = new RegistrationOutcome(RegistrationOutcome.REGISTERED, id);
                audit(c, actor, "REGISTER", "REGISTRATION", studentId + ":" + code, "registration_id=" + id);
            } else {
                long id = insertReturningKey(c, "INSERT INTO waiting_list (student_id, course_code) VALUES (?, ?)", studentId, code);
                outcome = new RegistrationOutcome(RegistrationOutcome.WAITLISTED, id);
                audit(c, actor, "WAITLIST", "WAITLIST", studentId + ":" + code, "queue_id=" + id);
            }
            return outcome;
        });
    }

    @Override public Optional<String> withdraw(String actor, String studentId, String courseCode) {
        String code = courseCode == null ? null : courseCode.trim().toUpperCase();
        return inTransaction(c -> {
            lockCourse(c, code);
            int changed = update(c, "UPDATE registrations SET status = 'WITHDRAWN' WHERE student_id = ? AND course_code = ? AND status = 'ACTIVE'",
                    studentId, code);
            if (changed == 0) {
                if (update(c, "DELETE FROM waiting_list WHERE student_id = ? AND course_code = ?", studentId, code) == 0)
                    throw new IllegalStateException("Student is not registered or waiting for " + code);
                audit(c, actor, "REMOVE_WAITLIST", "WAITLIST", studentId + ":" + code, "");
                return Optional.<String>empty();
            }
            audit(c, actor, "WITHDRAW", "REGISTRATION", studentId + ":" + code, "");
            record Head(long queueId, String studentId) {}
            List<Head> head = query(c, "SELECT queue_id, student_id FROM waiting_list WHERE course_code = ? ORDER BY queued_at, queue_id LIMIT 1 FOR UPDATE",
                    rs -> new Head(rs.getLong(1), rs.getString(2)), code);
            if (head.isEmpty()) return Optional.<String>empty();
            String next = head.get(0).studentId();
            update(c, "DELETE FROM waiting_list WHERE queue_id = ?", head.get(0).queueId());
            update(c, "INSERT INTO registrations (student_id, course_code, status) VALUES (?, ?, 'ACTIVE') "
                    + "ON DUPLICATE KEY UPDATE status = 'ACTIVE', created_at = CURRENT_TIMESTAMP", next, code);
            audit(c, actor, "PROMOTE", "REGISTRATION", next + ":" + code, "from waiting list");
            return Optional.of(next);
        });
    }

    @Override public List<Registration> findByStudent(String studentId) {
        return query("SELECT student_id, course_code, status, created_at FROM registrations WHERE student_id = ? ORDER BY course_code",
                rs -> new Registration(rs.getString(1), rs.getString(2), Registration.Status.valueOf(rs.getString(3)), ts(rs, "created_at")), studentId);
    }
    @Override public List<WaitlistEntry> waitingList(String courseCode) {
        List<WaitlistEntry> rows = new java.util.ArrayList<>();
        List<Object[]> raw = query("SELECT student_id, course_code, queued_at FROM waiting_list WHERE course_code = ? ORDER BY queued_at, queue_id",
                rs -> new Object[]{rs.getString(1), rs.getString(2), ts(rs, "queued_at")}, courseCode);
        int pos = 1;
        for (Object[] r : raw) rows.add(new WaitlistEntry((String) r[0], (String) r[1], pos++, (java.time.LocalDateTime) r[2]));
        return List.copyOf(rows);
    }
    @Override public int enrolledCount(String courseCode) {
        return query("SELECT COUNT(*) FROM registrations WHERE course_code = ? AND status = 'ACTIVE'", rs -> rs.getInt(1), courseCode).get(0);
    }
}
