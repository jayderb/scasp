package com.scasp.web;

import com.scasp.persistence.*;
import javax.sql.DataSource;

/** Repository instances shared by the servlets (stateless and thread-safe; each call borrows a pooled connection). */
public final class Repositories {
    public static final String CONTEXT_KEY = "scasp.repositories";
    public final JdbcCourseRepository courses;
    public final JdbcStudentRepository students;
    public final JdbcRegistrationRepository registrations;
    public final JdbcServiceRequestRepository requests;
    public final JdbcAnnouncementRepository announcements;
    public final JdbcAuditRepository audit;
    public final JdbcReportRepository reports;
    public final Authenticator authenticator;

    public Repositories(DataSource ds) {
        courses = new JdbcCourseRepository(ds); students = new JdbcStudentRepository(ds);
        registrations = new JdbcRegistrationRepository(ds); requests = new JdbcServiceRequestRepository(ds);
        announcements = new JdbcAnnouncementRepository(ds); audit = new JdbcAuditRepository(ds); reports = new JdbcReportRepository(ds);
        authenticator = new Authenticator(students);
    }
}
