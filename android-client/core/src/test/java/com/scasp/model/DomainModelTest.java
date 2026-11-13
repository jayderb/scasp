package com.scasp.model;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.jupiter.api.Assertions.*;

class DomainModelTest {
    @Test void usersAreEqualByIdOnly() {
        User a = new User("U1", "Ann", Role.STAFF), b = new User("U1", "Other name", Role.ADMIN);
        assertEquals(a, b); assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, new User("U2", "Ann", Role.STAFF));
        assertEquals(1, new HashSet<>(Set.of(a)).stream().filter(b::equals).count());
    }
    @Test void studentIsUserWithStudentRole() {
        Student s = new Student("STU9", "Kim", "CS", 1);
        assertEquals(Role.STUDENT, s.getRole()); assertEquals("STU9", s.getStudentId()); assertFalse(s.isStaff());
        assertThrows(IllegalArgumentException.class, () -> new Student("STU9", "Kim", "CS", 0));
        assertThrows(IllegalArgumentException.class, () -> new Student("STU9", "Kim", " ", 1));
        assertThrows(IllegalArgumentException.class, () -> new User(" ", "x", Role.STAFF));
    }
    @Test void courseEqualityAndOrderingAreConsistent() {
        Course a = new Course("csc301", "Advanced Java", 30), b = new Course("CSC301", "Renamed", 10);
        assertEquals("CSC301", a.getCourseCode());
        assertEquals(a, b); assertEquals(a.hashCode(), b.hashCode()); assertEquals(0, a.compareTo(b));
        TreeSet<Course> set = new TreeSet<>(); set.add(a); set.add(b); set.add(new Course("AAA100", "First", 5));
        assertEquals(2, set.size()); assertEquals("AAA100", set.first().getCourseCode());
    }
    @Test void invalidCoursesRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Course("C", "t", 1));
        assertThrows(IllegalArgumentException.class, () -> new Course("CSC 301", "t", 1));
        assertThrows(IllegalArgumentException.class, () -> new Course("CSC301", "", 1));
        assertThrows(IllegalArgumentException.class, () -> new Course("CSC301", "t", 0));
    }
    @Test void courseUpdateKeepsIdentityAndStatus() {
        Course c = new Course("CSC301", "Java", 2); c.setActive(false);
        Course u = c.withDetails("Advanced Java", 5);
        assertEquals(c, u); assertEquals(5, u.getCapacity()); assertEquals(Course.Status.INACTIVE, u.getStatus());
        assertEquals(2, c.getCapacity(), "original unchanged");
    }
    @Test void registrationEqualityIgnoresStatusAndTime() {
        Registration r1 = new Registration("S1", "C1", Registration.Status.ACTIVE, LocalDateTime.of(2026, 1, 1, 9, 0));
        Registration r2 = new Registration("S1", "C1", Registration.Status.WITHDRAWN);
        assertEquals(r1, r2); assertEquals(r1.hashCode(), r2.hashCode());
        Set<Registration> set = new HashSet<>(); assertTrue(set.add(r1)); assertFalse(set.add(r2), "duplicate rejected by Set");
        assertNotEquals(r1, new Registration("S1", "C2", Registration.Status.ACTIVE));
    }
    @Test void serviceRequestFollowsStateMachine() {
        ServiceRequest r = new ServiceRequest("S1", "transcript", "Need transcript", 3);
        assertEquals(ServiceRequest.Status.OPEN, r.getStatus()); assertEquals("TRANSCRIPT", r.getCategory());
        assertThrows(IllegalStateException.class, () -> r.updateStatus(ServiceRequest.Status.RESOLVED), "OPEN->RESOLVED invalid");
        assertThrows(IllegalStateException.class, () -> r.updateStatus(ServiceRequest.Status.IN_PROGRESS), "OPEN->IN_PROGRESS invalid");
        r.assign("STAFF01");
        assertEquals(ServiceRequest.Status.ASSIGNED, r.getStatus()); assertEquals("STAFF01", r.getAssignedTo());
        assertThrows(IllegalStateException.class, () -> r.assign("STAFF02"), "cannot re-assign");
        r.updateStatus(ServiceRequest.Status.IN_PROGRESS);
        r.updateStatus(ServiceRequest.Status.RESOLVED);
        assertTrue(r.isClosed());
        assertThrows(IllegalStateException.class, () -> r.updateStatus(ServiceRequest.Status.REJECTED), "closed is final");
        assertTrue(ServiceRequest.allowedNext(ServiceRequest.Status.RESOLVED).isEmpty());
    }
    @Test void invalidServiceRequestsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ServiceRequest("S1", "X", "d", 0));
        assertThrows(IllegalArgumentException.class, () -> new ServiceRequest("S1", "X", "d", 6));
        assertThrows(IllegalArgumentException.class, () -> new ServiceRequest(null, "X", "d", 3));
    }
    @Test void requestIdsAreStableAndUnique() {
        ServiceRequest a = new ServiceRequest("S1", "X", "d", 3), b = new ServiceRequest("S1", "X", "d", 3);
        assertNotEquals(a, b); assertNotEquals(a.getRequestId(), b.getRequestId());
        assertEquals(a.getRequestId(), a.getRequestId());
        assertTrue(b.getSequence() > a.getSequence());
    }
    @Test void auditEventRequiresActorAndEntity() {
        LocalDateTime t = LocalDateTime.now();
        assertThrows(NullPointerException.class, () -> new AuditEvent(1, null, "A", "COURSE", "C1", "", t));
        assertThrows(IllegalArgumentException.class, () -> new AuditEvent(0, "u", "A", "COURSE", "C1", "", t));
        AuditEvent e = new AuditEvent(1, "STAFF01", "ADD_COURSE", "COURSE", "CSC301", null, t);
        assertEquals("", e.details()); assertTrue(e.toString().contains("STAFF01 | ADD_COURSE | COURSE:CSC301"));
    }
    @Test void announcementAudienceRules() {
        assertTrue(Announcement.Audience.ALL.includes(Role.STUDENT));
        assertTrue(Announcement.Audience.STUDENTS.includes(Role.STUDENT));
        assertFalse(Announcement.Audience.STUDENTS.includes(Role.STAFF));
        assertTrue(Announcement.Audience.STAFF.includes(Role.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> new Announcement("A1", " ", "b", Announcement.Audience.ALL, LocalDateTime.now()));
    }
    @Test void campusServiceValidatesCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new CampusService("L", "Lib", "d", 91, 0));
        assertThrows(IllegalArgumentException.class, () -> new CampusService("L", "Lib", "d", 0, 181));
        assertEquals(-15.4, new CampusService("L", "Lib", "d", -15.4, 28.3).latitude());
    }
}
