package com.scasp.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Academic service request (transcript, clearance, appeal ...). Status changes follow an explicit
 * state machine; illegal transitions throw IllegalStateException. Mutators are synchronized because
 * staff may process requests from several threads.
 */
public class ServiceRequest {
    public enum Status { OPEN, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED }
    public enum Category { TRANSCRIPT, CLEARANCE, APPEAL, REGISTRATION, OTHER }

    private static final Map<Status, Set<Status>> ALLOWED = new EnumMap<>(Status.class);
    static {
        ALLOWED.put(Status.OPEN, EnumSet.of(Status.ASSIGNED, Status.REJECTED));
        ALLOWED.put(Status.ASSIGNED, EnumSet.of(Status.IN_PROGRESS, Status.REJECTED));
        ALLOWED.put(Status.IN_PROGRESS, EnumSet.of(Status.RESOLVED, Status.REJECTED));
        ALLOWED.put(Status.RESOLVED, EnumSet.noneOf(Status.class));
        ALLOWED.put(Status.REJECTED, EnumSet.noneOf(Status.class));
    }
    private static final AtomicLong SEQUENCE = new AtomicLong();

    private final String requestId;
    private final long sequence = SEQUENCE.incrementAndGet(); // FIFO tie-breaker for equal priority
    private final String studentId, category, description;
    private final int priority;
    private Status status = Status.OPEN;
    private String assignedTo = "";
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ServiceRequest(String studentId, String category, String description, int priority) {
        this(UUID.randomUUID().toString(), studentId, category, description, priority, LocalDateTime.now());
    }
    /** Restores a request with a known identifier (e.g. a database generated key). */
    public ServiceRequest(String requestId, String studentId, String category, String description, int priority, LocalDateTime createdAt) {
        if (requestId == null || requestId.isBlank() || studentId == null || studentId.isBlank() || category == null || category.isBlank()
                || description == null || description.isBlank() || priority < 1 || priority > 5 || createdAt == null)
            throw new IllegalArgumentException("Invalid service request");
        this.requestId = requestId; this.studentId = studentId; this.category = category.trim().toUpperCase();
        this.description = description.trim(); this.priority = priority; this.createdAt = createdAt; this.updatedAt = createdAt;
    }
    public String getRequestId() { return requestId; }
    public long getSequence() { return sequence; }
    public String getStudentId() { return studentId; }
    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public int getPriority() { return priority; }
    public synchronized Status getStatus() { return status; }
    public synchronized String getAssignedTo() { return assignedTo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public synchronized LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Set<Status> allowedNext(Status from) { return Collections.unmodifiableSet(ALLOWED.get(from)); }
    public static boolean canTransition(Status from, Status to) { return ALLOWED.get(from).contains(to); }

    public synchronized void assign(String staffId) {
        if (staffId == null || staffId.isBlank()) throw new IllegalArgumentException("Staff id is required");
        transitionTo(Status.ASSIGNED);
        assignedTo = staffId;
    }
    public synchronized void updateStatus(Status next) {
        if (next == Status.ASSIGNED) throw new IllegalStateException("Use assign(staffId) to assign a request");
        transitionTo(next);
    }
    private void transitionTo(Status next) {
        if (next == null || !canTransition(status, next))
            throw new IllegalStateException("Invalid transition " + status + " -> " + next);
        status = next; updatedAt = LocalDateTime.now();
    }
    public synchronized boolean isClosed() { return status == Status.RESOLVED || status == Status.REJECTED; }
    @Override public boolean equals(Object o) { return o instanceof ServiceRequest r && requestId.equals(r.requestId); }
    @Override public int hashCode() { return requestId.hashCode(); }
    @Override public synchronized String toString() {
        return requestId + " [" + category + ", p" + priority + ", " + status + (assignedTo.isEmpty() ? "" : ", " + assignedTo) + "]";
    }
}
