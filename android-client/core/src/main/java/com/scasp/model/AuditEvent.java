package com.scasp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable audit record: who (actor) did what (action) to which entity (type + id) and when. */
public record AuditEvent(long eventId, String actor, String action, String entityType, String entityId,
                         String details, LocalDateTime occurredAt) {
    public AuditEvent {
        if (eventId < 1) throw new IllegalArgumentException("eventId must be positive");
        Objects.requireNonNull(actor, "actor"); Objects.requireNonNull(action, "action");
        Objects.requireNonNull(entityType, "entityType"); Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(occurredAt, "occurredAt");
        details = details == null ? "" : details;
    }
    @Override public String toString() {
        return eventId + " | " + occurredAt + " | " + actor + " | " + action + " | " + entityType + ":" + entityId
                + (details.isEmpty() ? "" : " | " + details);
    }
}
