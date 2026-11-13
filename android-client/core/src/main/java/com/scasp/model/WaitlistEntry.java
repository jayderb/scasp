package com.scasp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable waiting-list entry; FIFO position is given by the Deque order (and createdAt in the database). */
public record WaitlistEntry(String studentId, String courseCode, int position, LocalDateTime createdAt) {
    public WaitlistEntry {
        Objects.requireNonNull(studentId); Objects.requireNonNull(courseCode); Objects.requireNonNull(createdAt);
        if (position < 1) throw new IllegalArgumentException("Position starts at 1");
    }
}
