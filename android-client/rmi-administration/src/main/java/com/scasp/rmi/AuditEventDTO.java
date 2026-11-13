package com.scasp.rmi;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record AuditEventDTO(long eventId, String actor, String action, String entity, LocalDateTime occurredAt) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
