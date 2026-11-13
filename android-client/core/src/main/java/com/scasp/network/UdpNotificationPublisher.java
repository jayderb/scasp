package com.scasp.network;

import com.scasp.model.AuditEvent;
import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Notification capability: turns registration/request audit events into UDP notification datagrams
 * ({@code EVENT|action|entityType|entityId|actor}). Register with AcademicService.addAuditListener.
 * Failures are swallowed: a lost notification must never roll back a registration.
 */
public final class UdpNotificationPublisher implements Consumer<AuditEvent> {
    private static final Set<String> NOTIFY = Set.of("REGISTER", "WAITLIST", "PROMOTE", "WITHDRAW", "SUBMIT_REQUEST",
            "ASSIGN_REQUEST", "REQUEST_STATUS", "PUBLISH_ANNOUNCEMENT");
    private final String host; private final int port;
    public UdpNotificationPublisher(String host, int port) { this.host = host; this.port = port; }
    @Override public void accept(AuditEvent e) {
        if (!NOTIFY.contains(e.action())) return;
        try { AnnouncementUdpClient.send(host, port, "EVENT|" + e.action() + "|" + e.entityType() + "|" + e.entityId() + "|" + e.actor()); }
        catch (IOException | RuntimeException ignored) { }
    }
}
