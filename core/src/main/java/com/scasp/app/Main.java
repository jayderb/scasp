package com.scasp.app;

import com.scasp.model.*;
import com.scasp.service.AcademicService;
import com.scasp.service.CourseComparators;

/** Stage 1 minimum demonstration: load data, fill a course, wait-list, withdraw, promote, reports. */
public class Main {
    public static void main(String[] args) {
        AcademicService service = SampleData.load(new AcademicService());
        System.out.println("SMART CAMPUS ACADEMIC SERVICES PLATFORM");
        System.out.println("Courses (by code):  " + service.listCourses());
        System.out.println("Courses (by title): " + service.listCourses(CourseComparators.BY_TITLE));
        System.out.println("STU001: " + service.register("STU001", "CSC301"));
        System.out.println("STU002: " + service.register("STU002", "CSC301"));
        System.out.println("STU003: " + service.register("STU003", "CSC301"));
        System.out.println("STU004: " + service.register("STU004", "CSC301"));
        System.out.println("Waiting list: " + service.waitingListEntries("CSC301"));
        service.withdraw("STU001", "CSC301");
        System.out.println("After STU001 withdrew, enrolled=" + service.enrolledStudents("CSC301"));
        System.out.println("Waiting list: " + service.waitingList("CSC301"));

        service.submitRequest(new ServiceRequest("STU002", "TRANSCRIPT", "Official transcript for bursary", 2));
        service.submitRequest(new ServiceRequest("STU003", "APPEAL", "Exam result appeal", 5));
        System.out.println("Request queue (priority order): " + service.openRequestQueue("STAFF01"));
        System.out.println("STAFF01 takes: " + service.assignNextRequest("STAFF01").orElseThrow());
        service.publishAnnouncement("STAFF01", "Registration reminder", "Registration closes Friday.", Announcement.Audience.STUDENTS);
        System.out.println("Student announcements: " + service.announcementsFor(Role.STUDENT));

        System.out.println("\n-- Reports (staff) --");
        service.capacityReport("STAFF01").forEach(r -> System.out.printf("%-7s %-26s %d/%d waiting=%d%n",
                r.courseCode(), r.title(), r.enrolled(), r.capacity(), r.waiting()));
        System.out.println("Waiting lists: " + service.waitingListReport("STAFF01"));
        System.out.println("Student registrations: " + service.studentRegistrationReport("STAFF01"));
        System.out.println("Request workload: " + service.requestWorkloadReport("STAFF01"));
        System.out.println("Audit counts: " + service.auditReport("STAFF01"));
        System.out.println("\n-- Audit trail --");
        service.auditTrail().forEach(System.out::println);
    }
}
