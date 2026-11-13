package com.scasp.persistence;

import com.scasp.model.*;
import com.scasp.report.*;
import com.scasp.repository.DataAccessException;
import com.scasp.repository.RegistrationOutcome;
import com.scasp.repository.UserAccount;
import com.scasp.security.PasswordHasher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@Timeout(60)
class JdbcRepositoryIntegrationTest {
    DataSource ds;
    JdbcCourseRepository courses; JdbcStudentRepository students; JdbcRegistrationRepository registrations;
    JdbcServiceRequestRepository requests; JdbcAnnouncementRepository announcements; JdbcAuditRepository audit; JdbcReportRepository reports;

    @BeforeEach void reset() {
        ds = TestDatabase.dataSourceOrSkip();
        SqlScriptRunner.resetDatabase(ds);   // repeatable scripts: drop + create + sample data
        courses = new JdbcCourseRepository(ds); students = new JdbcStudentRepository(ds);
        registrations = new JdbcRegistrationRepository(ds); requests = new JdbcServiceRequestRepository(ds);
        announcements = new JdbcAnnouncementRepository(ds); audit = new JdbcAuditRepository(ds); reports = new JdbcReportRepository(ds);
    }

    // ------------------------------------------------------------- CRUD
    @Test void courseCrud() {
        courses.create("STAFF01", new Course("NET303", "Network Programming", 4));
        assertEquals("Network Programming", courses.findByCode("net303").orElseThrow().getTitle());
        courses.update("STAFF01", new Course("NET303", "Networks", 6));
        assertEquals(6, courses.findByCode("NET303").orElseThrow().getCapacity());
        courses.setActive("STAFF01", "NET303", false);
        assertFalse(courses.findByCode("NET303").orElseThrow().isActive());
        assertEquals(List.of("CSC301", "DB302", "HCI308"), courses.search("", true).stream().map(Course::getCourseCode).toList());
        assertEquals(List.of("CSC301"), courses.search("java", false).stream().map(Course::getCourseCode).toList());
        assertTrue(courses.search("%", false).isEmpty(), "LIKE wildcards typed by a user are escaped");
        assertTrue(courses.delete("NET303"));
        assertTrue(courses.findByCode("NET303").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> courses.update("STAFF01", new Course("NOPE99", "x", 1)));
    }
    @Test void studentCrudAndAccounts() {
        students.create(new Student("STU900", "Test Student", "Data Science", 1), PasswordHasher.hash("pw-123456".toCharArray()));
        assertEquals("Data Science", students.findById("STU900").orElseThrow().getProgramme());
        students.update(new Student("STU900", "Renamed Student", "Data Science", 2));
        assertEquals(2, students.findById("STU900").orElseThrow().getYearOfStudy());
        assertEquals(5, students.findAll().size());
        UserAccount acc = students.findAccount("STU900").orElseThrow();
        assertEquals(Role.STUDENT, acc.role());
        assertTrue(PasswordHasher.verify("pw-123456".toCharArray(), acc.passwordHash()));
        assertFalse(acc.toString().contains("pbkdf2"), "hash never printed");
        assertTrue(students.delete("STU900"));
        assertTrue(students.findById("STU900").isEmpty());
        assertTrue(PasswordHasher.verify("staff123".toCharArray(), students.findAccount("STAFF01").orElseThrow().passwordHash()),
                "sample-data hashes match the documented demo passwords");
    }

    // ------------------------------------------------- transactions
    @Test void registrationTransactionCommitsRegistrationWaitlistAndAudit() {
        RegistrationOutcome a = registrations.register("STU001", "STU001", "CSC301");
        RegistrationOutcome b = registrations.register("STU002", "STU002", "CSC301");
        RegistrationOutcome c = registrations.register("STU003", "STU003", "CSC301");
        assertEquals("REGISTERED", a.status()); assertEquals("REGISTERED", b.status()); assertEquals("WAITLISTED", c.status());
        assertTrue(b.generatedId() > a.generatedId(), "generated keys returned");
        assertEquals(2, registrations.enrolledCount("CSC301"));
        assertEquals("STU003", registrations.waitingList("CSC301").get(0).studentId());
        assertEquals(1, audit.findByEntity("REGISTRATION", "STU001:CSC301").size());
        assertEquals("WAITLIST", audit.findByEntity("WAITLIST", "STU003:CSC301").get(0).action());

        assertEquals(Optional.of("STU003"), registrations.withdraw("STU001", "STU001", "CSC301"));
        assertEquals(2, registrations.enrolledCount("CSC301"));
        assertTrue(registrations.waitingList("CSC301").isEmpty());
        assertEquals(Registration.Status.WITHDRAWN, registrations.findByStudent("STU001").get(0).status());
        assertEquals("REGISTERED", registrations.register("STU004", "STU004", "DB302").status());
        assertEquals("WAITLISTED", registrations.register("STU001", "STU001", "CSC301").status(), "re-join after withdrawal");
    }
    @Test void businessRuleViolationsRollBackAndLeaveNoTrace() {
        registrations.register("STU001", "STU001", "CSC301");
        long auditBefore = countRows("audit_event");
        assertThrows(IllegalStateException.class, () -> registrations.register("STU001", "STU001", "CSC301"));
        assertThrows(IllegalArgumentException.class, () -> registrations.register("STU001", "NOBODY", "CSC301"));
        assertThrows(IllegalArgumentException.class, () -> registrations.register("STU001", "STU001", "NOPE99"));
        assertThrows(IllegalStateException.class, () -> registrations.withdraw("STU002", "STU002", "CSC301"));
        assertEquals(auditBefore, countRows("audit_event"), "nothing written by failed operations");
    }
    @Test void failureAfterInsertRollsBackTheWholeUnit() {
        // actor NULL violates audit_event.actor NOT NULL *after* the registration INSERT has executed:
        DataAccessException e = assertThrows(DataAccessException.class, () -> registrations.register(null, "STU002", "DB302"));
        assertTrue(e.isConstraintViolation(), "SQLState class 23: " + e.getSqlState());
        assertEquals(0, registrations.enrolledCount("DB302"), "registration insert was rolled back");
        assertTrue(registrations.findByStudent("STU002").isEmpty());
        // the pooled connection was returned in auto-commit mode and still works
        assertEquals("REGISTERED", registrations.register("STU002", "STU002", "DB302").status());
    }
    @Test void withdrawPromotionRollsBackTogether() {
        registrations.register("STU001", "STU001", "CSC301"); registrations.register("STU002", "STU002", "CSC301");
        registrations.register("STU003", "STU003", "CSC301");
        assertThrows(DataAccessException.class, () -> registrations.withdraw(null, "STU001", "CSC301"));
        assertEquals(List.of("STU001", "STU002"), activeStudents("CSC301"), "withdrawal undone");
        assertEquals(1, registrations.waitingList("CSC301").size(), "promotion undone");
    }
    @Test void constraintsAreEnforcedByTheDatabase() {
        DataAccessException fk = assertThrows(DataAccessException.class, () -> {
            registrations.register("STU001", "STU001", "CSC301");
            courses.delete("CSC301");
        });
        assertTrue(fk.isConstraintViolation());
        DataAccessException dup = assertThrows(DataAccessException.class, () -> courses.create("STAFF01", new Course("CSC301", "Dup", 3)));
        assertTrue(dup.isConstraintViolation());
        DataAccessException check = assertThrows(DataAccessException.class, () -> rawUpdate("UPDATE courses SET capacity = 0 WHERE course_code = 'DB302'"));
        assertNotNull(check.getSqlState());
        assertThrows(IllegalStateException.class, () -> {
            registrations.register("STU002", "STU002", "CSC301");
            courses.update("STAFF01", new Course("CSC301", "Advanced Java", 1));
        }, "capacity below enrolment");
    }
    @Test void concurrentRegistrationsAreSerialisedByRowLock() throws Exception {
        for (int i = 0; i < 12; i++) students.create(new Student("LOAD" + i, "Load " + i, "CS", 1), null);
        CyclicBarrier start = new CyclicBarrier(12);
        ExecutorService ex = Executors.newFixedThreadPool(12);
        List<Future<String>> fs = new ArrayList<>();
        for (int i = 0; i < 12; i++) { String id = "LOAD" + i;
            fs.add(ex.submit(() -> { start.await(); return registrations.register(id, id, "DB302").status(); })); }
        int reg = 0, wait = 0;
        for (Future<String> f : fs) if (f.get().equals("REGISTERED")) reg++; else wait++;
        ex.shutdown();
        assertEquals(3, reg); assertEquals(9, wait);
        assertEquals(3, registrations.enrolledCount("DB302"));
    }

    // ------------------------------------------------- requests, announcements
    @Test void serviceRequestLifecycleWithGeneratedKeys() {
        long id1 = requests.create("STU001", "STU001", "transcript", "Need transcript", 2);
        long id2 = requests.create("STU002", "STU002", "APPEAL", "Exam appeal", 5);
        assertTrue(id2 > id1);
        assertEquals(List.of(String.valueOf(id2), String.valueOf(id1)), requests.findOpenByPriority().stream().map(ServiceRequestSummary::requestId).toList());
        assertThrows(IllegalStateException.class, () -> requests.updateStatus("STAFF01", id1, ServiceRequest.Status.RESOLVED));
        assertThrows(IllegalArgumentException.class, () -> requests.assign("STAFF01", id1, "STU002"));
        assertEquals("STAFF01", requests.assign("STAFF01", id1, "STAFF01").assignedTo());
        requests.updateStatus("STAFF01", id1, ServiceRequest.Status.IN_PROGRESS);
        assertEquals(ServiceRequest.Status.RESOLVED, requests.updateStatus("STAFF01", id1, ServiceRequest.Status.RESOLVED).status());
        assertEquals(1, requests.findByStudent("STU001").size());
        assertThrows(IllegalArgumentException.class, () -> requests.create("STU001", "STU001", "X", "d", 9));
        assertEquals(4, audit.findByEntity("SERVICE_REQUEST", String.valueOf(id1)).size());
    }
    @Test void announcementsByAudience() {
        announcements.publish("STAFF01", "Staff meeting", "Monday 10:00", Announcement.Audience.STAFF);
        announcements.publish("STAFF01", "Campus closed", "Public holiday", Announcement.Audience.ALL);
        assertEquals(List.of("Campus closed", "Registration reminder"), announcements.findVisibleTo(Role.STUDENT).stream().map(Announcement::title).toList());
        assertEquals(List.of("Campus closed", "Staff meeting"), announcements.findVisibleTo(Role.STAFF).stream().map(Announcement::title).toList());
    }

    // ------------------------------------------------- reports (joins)
    @Test void joinReports() {
        registrations.register("STU001", "STU001", "CSC301"); registrations.register("STU002", "STU002", "CSC301");
        registrations.register("STU003", "STU003", "CSC301"); registrations.register("STU004", "STU004", "CSC301");
        registrations.register("STU001", "STU001", "DB302");
        long rid = requests.create("STU001", "STU001", "TRANSCRIPT", "t", 3); requests.assign("STAFF01", rid, "STAFF01");
        requests.create("STU002", "STU002", "APPEAL", "a", 5);

        CapacityRow csc = reports.capacityReport().get(0);
        assertEquals("CSC301", csc.courseCode()); assertEquals(2, csc.enrolled()); assertEquals(2, csc.waiting()); assertEquals(0, csc.seatsRemaining());
        List<StudentRegistrationRow> sr = reports.studentRegistrationReport();
        assertEquals(3, sr.size()); assertEquals("Alex Banda", sr.get(0).fullName());
        List<WaitingListRow> wl = reports.waitingListReport();
        assertEquals(List.of(1, 2), wl.stream().map(WaitingListRow::position).toList());
        assertEquals("John Zulu", wl.get(0).fullName());
        List<WorkloadRow> wk = reports.requestWorkloadReport();
        assertTrue(wk.stream().anyMatch(r -> r.assignee().equals("STAFF01") && r.assigneeName().equals("Grace Tembo") && r.requests() == 1));
        assertTrue(wk.stream().anyMatch(r -> r.assignee().equals("(unassigned)") && r.status().equals("OPEN")));
        List<AuditActorRow> ar = reports.auditByActorReport();
        assertTrue(ar.stream().anyMatch(r -> r.actor().equals("SYSTEM") && r.role().equals("SYSTEM")));
        assertTrue(ar.stream().anyMatch(r -> r.actor().equals("STU001") && r.actorName().equals("Alex Banda")));
    }

    @Test void sqlInjectionAttemptIsTreatedAsData() {
        String evil = "CSC301' OR '1'='1";
        assertTrue(courses.findByCode(evil).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> registrations.register("STU001", "STU001", "x'; DROP TABLE courses; --"));
        assertEquals(3, courses.findAll().size(), "table intact");
    }

    // ------------------------------------------------- helpers
    private long countRows(String table) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM " + table); ResultSet rs = ps.executeQuery()) {
            rs.next(); return rs.getLong(1);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private void rawUpdate(String sql) {
        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) { ps.executeUpdate(); }
        catch (java.sql.SQLException e) { throw new DataAccessException(e.getMessage(), e, e.getSQLState()); }
    }
    private List<String> activeStudents(String code) {
        return registrations.findByStudent("STU001").isEmpty() ? List.of() :
                reports.studentRegistrationReport().stream().filter(r -> r.courseCode().equals(code)).map(StudentRegistrationRow::studentId).toList();
    }
}
