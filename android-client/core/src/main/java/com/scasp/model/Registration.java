package com.scasp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable registration value. Equality is (studentId, courseCode) only, so a Set of registrations
 * can never hold the same student twice in the same course regardless of status/time.
 */
public record Registration(String studentId, String courseCode, Status status, LocalDateTime createdAt) {
    public enum Status { ACTIVE, WITHDRAWN }
    public Registration {
        Objects.requireNonNull(studentId, "studentId"); Objects.requireNonNull(courseCode, "courseCode");
        Objects.requireNonNull(status, "status"); Objects.requireNonNull(createdAt, "createdAt");
    }
    public Registration(String studentId, String courseCode, Status status) {
        this(studentId, courseCode, status, LocalDateTime.now());
    }
    @Override public boolean equals(Object o) {
        return o instanceof Registration r && studentId.equals(r.studentId) && courseCode.equals(r.courseCode);
    }
    @Override public int hashCode() { return Objects.hash(studentId, courseCode); }
}
