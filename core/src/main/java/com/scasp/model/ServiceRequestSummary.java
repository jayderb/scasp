package com.scasp.model;

import java.time.LocalDateTime;

/** Immutable snapshot of a ServiceRequest returned across service boundaries (defensive copy). */
public record ServiceRequestSummary(String requestId, String studentId, String category, String description, int priority,
                                    ServiceRequest.Status status, String assignedTo, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static ServiceRequestSummary of(ServiceRequest r) {
        synchronized (r) {
            return new ServiceRequestSummary(r.getRequestId(), r.getStudentId(), r.getCategory(), r.getDescription(), r.getPriority(),
                    r.getStatus(), r.getAssignedTo(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
