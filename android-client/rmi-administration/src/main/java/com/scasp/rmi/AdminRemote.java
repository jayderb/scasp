package com.scasp.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote administration/reporting interface (Stage 8). Scope is deliberately limited to read-only reports
 * plus two staff administrative actions; no persistence objects (Connections, ResultSets, entities) are exposed.
 * All results are Serializable DTOs / JDK collections (pass-by-value). The AdminRemote stub itself is a
 * remote reference (pass-by-reference).
 */
public interface AdminRemote extends Remote {
    /** Original operations (kept). */
    List<String> listCourses() throws RemoteException, RemoteAdminException;
    int countRegistrations(String courseCode) throws RemoteException, RemoteAdminException;
    List<String> getWaitingList(String courseCode) throws RemoteException, RemoteAdminException;
    List<String> getAuditReport() throws RemoteException, RemoteAdminException;
    /** New DTO-based operations. */
    List<CourseSummaryDTO> getCapacityReport(String staffId) throws RemoteException, RemoteAdminException;
    Map<String, Long> getRequestWorkload(String staffId) throws RemoteException, RemoteAdminException;
    List<AuditEventDTO> getRecentAudit(String staffId, int limit) throws RemoteException, RemoteAdminException;
    /** Administrative actions (staff role required; violations come back as RemoteAdminException). */
    void setCourseActive(String staffId, String courseCode, boolean active) throws RemoteException, RemoteAdminException;
    String publishAnnouncement(String staffId, String title, String body, String audience) throws RemoteException, RemoteAdminException;
}
