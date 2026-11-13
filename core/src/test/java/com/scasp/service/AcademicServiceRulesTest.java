package com.scasp.service;

import com.scasp.app.SampleData;
import com.scasp.model.*;
import com.scasp.report.CapacityRow;
import com.scasp.report.RequestWorkload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AcademicServiceRulesTest {
    AcademicService s;
    @BeforeEach void setUp() { s = SampleData.load(new AcademicService(Clock.fixed(Instant.parse("2026-11-13T08:00:00Z"), ZoneOffset.UTC))); }

    // ---- registration, capacity, waiting list
    @Test void capacityIsEnforcedAndWaitingListIsFifo() {
        assertEquals("REGISTERED", s.register("STU001", "CSC301"));
        assertEquals("REGISTERED", s.register("STU002", "CSC301"));
        assertEquals("WAITLISTED", s.register("STU003", "CSC301"));
        assertEquals("WAITLISTED", s.register("STU004", "CSC301"));
        assertEquals(2, s.enrolledCount("CSC301"));
        assertEquals(List.of("STU003", "STU004"), s.waitingList("CSC301"));
        assertEquals(2, s.waitingListEntries("CSC301").get(1).position());
        assertEquals("STU003", s.withdraw("STU001", "STU001", "CSC301").orElseThrow());
        assertEquals(List.of("STU002", "STU003"), s.enrolledStudents("CSC301"));
        assertEquals(List.of("STU004"), s.waitingList("CSC301"));
    }
    @Test void cannotWaitlistTwiceOrRegisterInactiveOrUnknown() {
        s.register("STU001", "CSC301"); s.register("STU002", "CSC301"); s.register("STU003", "CSC301");
        assertThrows(IllegalStateException.class, () -> s.register("STU003", "CSC301"));
        s.setCourseActive("STAFF01", "DB302", false);
        assertThrows(IllegalStateException.class, () -> s.register("STU001", "DB302"));
        assertThrows(IllegalArgumentException.class, () -> s.register("STU001", "NOPE99"));
        assertThrows(IllegalArgumentException.class, () -> s.register("STAFF01", "NOBODY", "CSC301"));
    }
    @Test void withdrawFromWaitingListAndInvalidWithdraw() {
        s.register("STU001", "CSC301"); s.register("STU002", "CSC301"); s.register("STU003", "CSC301");
        assertTrue(s.withdraw("STU003", "STU003", "CSC301").isEmpty());
        assertTrue(s.waitingList("CSC301").isEmpty());
        assertThrows(IllegalStateException.class, () -> s.withdraw("STU004", "STU004", "CSC301"));
    }
    @Test void reRegistrationAfterWithdrawalIsAllowed() {
        s.register("STU001", "DB302"); s.withdraw("STU001", "DB302");
        assertEquals("REGISTERED", s.register("STU001", "DB302"));
    }
    @Test void raisingCapacityPromotesWaitingStudents() {
        s.register("STU001", "CSC301"); s.register("STU002", "CSC301"); s.register("STU003", "CSC301");
        s.updateCourse("STAFF01", "CSC301", "Advanced Java", 3);
        assertEquals(3, s.enrolledCount("CSC301")); assertTrue(s.waitingList("CSC301").isEmpty());
        assertThrows(IllegalStateException.class, () -> s.updateCourse("STAFF01", "CSC301", "Advanced Java", 2), "below enrolment");
    }

    // ---- authorisation
    @Test void staffOnlyOperationsRejectStudents() {
        assertThrows(AuthorisationException.class, () -> s.createCourse("STU001", new Course("NEW101", "New", 5)));
        assertThrows(AuthorisationException.class, () -> s.updateCourse("STU001", "CSC301", "x", 9));
        assertThrows(AuthorisationException.class, () -> s.capacityReport("STU001"));
        assertThrows(AuthorisationException.class, () -> s.publishAnnouncement("STU001", "t", "b", Announcement.Audience.ALL));
        assertThrows(AuthorisationException.class, () -> s.assignNextRequest("STU001"));
        assertThrows(AuthorisationException.class, () -> s.auditReport("UNKNOWN"));
        s.createCourse("STAFF01", new Course("NEW101", "New", 5));
        assertTrue(s.findCourse("new101").isPresent());
    }
    @Test void studentsActOnlyForThemselvesStaffForAnyone() {
        assertThrows(AuthorisationException.class, () -> s.register("STU002", "STU001", "CSC301"));
        assertEquals("REGISTERED", s.register("STAFF01", "STU001", "CSC301"));
        assertThrows(AuthorisationException.class, () -> s.withdraw("STU002", "STU001", "CSC301"));
        assertThrows(AuthorisationException.class, () -> s.requestsFor("STU002", "STU001"));
    }
    @Test void suspendedUserIsRejected() {
        s.findUser("STAFF01").orElseThrow().setActive(false);
        assertThrows(AuthorisationException.class, () -> s.capacityReport("STAFF01"));
    }

    // ---- service requests
    @Test void requestsAreAssignedByPriorityThenFifo() {
        ServiceRequest low = new ServiceRequest("STU001", "TRANSCRIPT", "t", 1);
        ServiceRequest high1 = new ServiceRequest("STU002", "APPEAL", "a", 5);
        ServiceRequest high2 = new ServiceRequest("STU003", "CLEARANCE", "c", 5);
        s.submitRequest(low); s.submitRequest(high1); s.submitRequest(high2);
        assertEquals(List.of(high1.getRequestId(), high2.getRequestId(), low.getRequestId()),
                s.openRequestQueue("STAFF01").stream().map(ServiceRequestSummary::requestId).toList());
        assertEquals(high1.getRequestId(), s.assignNextRequest("STAFF01").orElseThrow().requestId());
        assertEquals(high2.getRequestId(), s.assignNextRequest("ADMIN01").orElseThrow().requestId());
        assertEquals(low.getRequestId(), s.assignNextRequest("STAFF01").orElseThrow().requestId());
        assertTrue(s.assignNextRequest("STAFF01").isEmpty());
    }
    @Test void requestTrackingAndInvalidTransitions() {
        ServiceRequest r = new ServiceRequest("STU001", "TRANSCRIPT", "t", 3);
        s.submitRequest(r);
        assertThrows(IllegalStateException.class, () -> s.updateRequestStatus("STAFF01", r.getRequestId(), ServiceRequest.Status.RESOLVED));
        assertThrows(IllegalArgumentException.class, () -> s.assignRequest("STAFF01", r.getRequestId(), "STU002"), "assignee must be staff");
        s.assignRequest("STAFF01", r.getRequestId(), "ADMIN01");
        assertTrue(s.openRequestQueue("STAFF01").isEmpty(), "assigned request left the queue");
        s.updateRequestStatus("ADMIN01", r.getRequestId(), ServiceRequest.Status.IN_PROGRESS);
        ServiceRequestSummary done = s.updateRequestStatus("ADMIN01", r.getRequestId(), ServiceRequest.Status.RESOLVED);
        assertEquals(ServiceRequest.Status.RESOLVED, done.status());
        assertEquals(1, s.requestsFor("STU001", "STU001").size());
        assertEquals(4, s.auditTrailFor("SERVICE_REQUEST", r.getRequestId()).size());
        assertThrows(IllegalArgumentException.class, () -> s.submitRequest(r), "duplicate request id");
    }
    @Test void rejectingOpenRequestRemovesItFromQueue() {
        ServiceRequest r = new ServiceRequest("STU001", "OTHER", "x", 2);
        s.submitRequest(r);
        s.updateRequestStatus("STAFF01", r.getRequestId(), ServiceRequest.Status.REJECTED);
        assertTrue(s.assignNextRequest("STAFF01").isEmpty());
    }

    // ---- announcements
    @Test void announcementsAreFilteredByAudienceNewestFirst() {
        s.publishAnnouncement("STAFF01", "Exams", "Timetable out", Announcement.Audience.STUDENTS);
        s.publishAnnouncement("STAFF01", "Meeting", "Staff meeting", Announcement.Audience.STAFF);
        s.publishAnnouncement("STAFF01", "Holiday", "Campus closed", Announcement.Audience.ALL);
        assertEquals(List.of("Holiday", "Exams"), s.announcementsFor(Role.STUDENT).stream().map(Announcement::title).toList());
        assertEquals(List.of("Holiday", "Meeting"), s.announcementsFor(Role.STAFF).stream().map(Announcement::title).toList());
        assertEquals(1, s.announcementsFor(Announcement.Audience.STAFF).size());
    }

    // ---- sorting / searching / defensive copies
    @Test void multipleComparatorsAndSearch() {
        assertEquals(List.of("CSC301", "DB302", "HCI308"), codes(s.listCourses()));
        assertEquals(List.of("DB302", "CSC301", "HCI308"), codes(s.listCourses(CourseComparators.BY_TITLE)));
        assertEquals(List.of("DB302", "CSC301", "HCI308"), codes(s.listCourses(CourseComparators.BY_CAPACITY_DESC)));
        s.register("STU001", "DB302"); s.register("STU002", "DB302"); s.register("STU003", "DB302");
        assertEquals(List.of("CSC301", "HCI308", "DB302"), codes(s.listCourses(CourseComparators.bySeatsRemainingDesc(
                Map.of("DB302", 3)))));
        assertEquals(List.of("CSC301"), codes(s.searchCourses("java", true, CourseComparators.BY_CODE)));
        assertEquals(List.of("CSC301", "DB302"), codes(s.searchCourses("advanced", true, CourseComparators.BY_CODE)));
        assertTrue(s.findCourse("bad code!").isEmpty());
        assertEquals("Advanced Database", s.findCourse("db302").orElseThrow().getTitle());
        assertEquals(List.of("Alex Banda", "Mary Phiri"), s.findStudentsByProgramme("computer science").stream().map(Student::getName).toList());
    }
    @Test void returnedCollectionsAreDefensive() {
        s.register("STU001", "CSC301");
        assertThrows(UnsupportedOperationException.class, () -> s.listCourses().add(new Course("X12", "x", 1)));
        assertThrows(UnsupportedOperationException.class, () -> s.waitingList("CSC301").add("STU009"));
        assertThrows(UnsupportedOperationException.class, () -> s.auditTrail().clear());
        assertThrows(UnsupportedOperationException.class, () -> s.studentRegistrationReport("STAFF01").put("x", List.of()));
        s.listCourses().get(0).setActive(false);  // mutate the copy
        assertTrue(s.findCourse("CSC301").orElseThrow().isActive(), "internal course unaffected");
    }

    // ---- reports
    @Test void reportsUseStreamsAndCollectors() {
        s.register("STU001", "CSC301"); s.register("STU002", "CSC301"); s.register("STU003", "CSC301"); s.register("STU001", "DB302");
        List<CapacityRow> cap = s.capacityReport("STAFF01");
        CapacityRow csc = cap.get(0);
        assertEquals("CSC301", csc.courseCode()); assertEquals(2, csc.enrolled()); assertEquals(1, csc.waiting());
        assertEquals(0, csc.seatsRemaining()); assertEquals(100.0, csc.utilisationPercent());
        assertEquals(Map.of("CSC301", List.of("STU003")), s.waitingListReport("STAFF01"));
        assertEquals(List.of("CSC301", "DB302"), s.studentRegistrationReport("STAFF01").get("STU001"));
        ServiceRequest r = new ServiceRequest("STU001", "TRANSCRIPT", "t", 4);
        s.submitRequest(r); s.submitRequest(new ServiceRequest("STU002", "APPEAL", "a", 2));
        s.assignNextRequest("STAFF01");
        RequestWorkload w = s.requestWorkloadReport("STAFF01");
        assertEquals(1L, w.byStatus().get(ServiceRequest.Status.OPEN));
        assertEquals(1L, w.byStatus().get(ServiceRequest.Status.ASSIGNED));
        assertEquals(Map.of("STAFF01", 1L), w.openByAssignee());
        assertEquals(3.0, w.averageOpenPriority());
        Map<String, Long> audit = s.auditReport("STAFF01");
        assertEquals(3L, audit.get("REGISTER"));
        assertEquals(1L, audit.get("WAITLIST"));
    }
    @Test void auditEventsRecordActorEntityAndTime() {
        s.register("STAFF01", "STU001", "CSC301");
        AuditEvent last = s.auditTrail().get(s.auditTrail().size() - 1);
        assertEquals("STAFF01", last.actor()); assertEquals("REGISTER", last.action());
        assertEquals("REGISTRATION", last.entityType()); assertEquals("STU001:CSC301", last.entityId());
        assertEquals(2026, last.occurredAt().getYear());
        List<Long> ids = s.auditTrail().stream().map(AuditEvent::eventId).toList();
        assertEquals(ids.stream().sorted().toList(), ids, "event ids increase");
    }
    @Test void auditListenersAreNotifiedAndCannotBreakOperations() {
        List<String> seen = new java.util.ArrayList<>();
        s.addAuditListener(e -> seen.add(e.action()));
        s.addAuditListener(e -> { throw new RuntimeException("listener failure"); });
        assertEquals("REGISTERED", s.register("STU001", "CSC301"));
        assertEquals(List.of("REGISTER"), seen);
    }
    private static List<String> codes(List<Course> cs) { return cs.stream().map(Course::getCourseCode).toList(); }
}
