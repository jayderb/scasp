package com.scasp.rmi;

import com.scasp.model.Announcement;
import com.scasp.model.AuditEvent;
import com.scasp.service.AcademicService;
import com.scasp.service.AuthorisationException;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.function.Supplier;

/** Remote object exported on an anonymous port and bound in the registry; delegates to the core AcademicService. */
public class AdminRemoteImpl extends UnicastRemoteObject implements AdminRemote {
    private static final long serialVersionUID = 1L;
    private final transient AcademicService service;

    public AdminRemoteImpl(AcademicService service) throws RemoteException { super(0); this.service = service; }

    private <T> T call(Supplier<T> op) throws RemoteException, RemoteAdminException {
        try { return op.get(); }
        catch (AuthorisationException e) { throw new RemoteAdminException("Not authorised: " + e.getMessage()); }
        catch (IllegalArgumentException | IllegalStateException e) { throw new RemoteAdminException(e.getMessage()); }
        catch (RuntimeException e) { throw new RemoteAdminException("Server error"); }
    }

    @Override public List<String> listCourses() throws RemoteException, RemoteAdminException {
        return call(() -> new ArrayList<>(service.listCourses().stream().map(c -> c.getCourseCode() + " - " + c.getTitle()).toList()));
    }
    @Override public int countRegistrations(String courseCode) throws RemoteException, RemoteAdminException { return call(() -> service.enrolledCount(courseCode)); }
    @Override public List<String> getWaitingList(String courseCode) throws RemoteException, RemoteAdminException { return call(() -> new ArrayList<>(service.waitingList(courseCode))); }
    @Override public List<String> getAuditReport() throws RemoteException, RemoteAdminException {
        return call(() -> new ArrayList<>(service.auditTrail().stream().map(AuditEvent::toString).toList()));
    }
    @Override public List<CourseSummaryDTO> getCapacityReport(String staffId) throws RemoteException, RemoteAdminException {
        return call(() -> new ArrayList<>(service.capacityReport(staffId).stream()
                .map(r -> new CourseSummaryDTO(r.courseCode(), r.title(), r.capacity(), r.enrolled(), r.waiting(), r.active())).toList()));
    }
    @Override public Map<String, Long> getRequestWorkload(String staffId) throws RemoteException, RemoteAdminException {
        return call(() -> {
            Map<String, Long> m = new TreeMap<>();
            service.requestWorkloadReport(staffId).byStatus().forEach((k, v) -> m.put(k.name(), v));
            return m;
        });
    }
    @Override public List<AuditEventDTO> getRecentAudit(String staffId, int limit) throws RemoteException, RemoteAdminException {
        return call(() -> {
            service.auditReport(staffId); // authorisation check (staff only)
            List<AuditEvent> all = service.auditTrail();
            int from = Math.max(0, all.size() - Math.max(1, Math.min(limit, 200)));
            return new ArrayList<>(all.subList(from, all.size()).stream()
                    .map(e -> new AuditEventDTO(e.eventId(), e.actor(), e.action(), e.entityType() + ":" + e.entityId(), e.occurredAt())).toList());
        });
    }
    @Override public void setCourseActive(String staffId, String courseCode, boolean active) throws RemoteException, RemoteAdminException {
        call(() -> { service.setCourseActive(staffId, courseCode, active); return null; });
    }
    @Override public String publishAnnouncement(String staffId, String title, String body, String audience) throws RemoteException, RemoteAdminException {
        return call(() -> service.publishAnnouncement(staffId, title, body, Announcement.Audience.valueOf(audience.toUpperCase())).announcementId());
    }
}
