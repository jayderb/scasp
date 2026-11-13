package com.scasp.report;
import java.time.LocalDateTime;
public record AuditActorRow(String actor, String actorName, String role, long events, LocalDateTime lastEventAt) {}
