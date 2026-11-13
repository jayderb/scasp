package com.scasp.concurrent;

import com.scasp.model.ServiceRequest;
import com.scasp.model.ServiceRequestSummary;
import com.scasp.report.CapacityRow;
import com.scasp.service.AcademicService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/** Business actions expressed as Callable tasks for the RequestProcessor (Stage 2 requirement 1). */
public final class ScaspTasks {
    private ScaspTasks() {}
    public static Callable<String> register(AcademicService s, String actor, String studentId, String courseCode) {
        return () -> s.register(actor, studentId, courseCode);
    }
    public static Callable<Optional<String>> withdraw(AcademicService s, String actor, String studentId, String courseCode) {
        return () -> s.withdraw(actor, studentId, courseCode);
    }
    public static Callable<List<CapacityRow>> capacityReport(AcademicService s, String staffId) {
        return () -> s.capacityReport(staffId);
    }
    public static Callable<ServiceRequestSummary> submitRequest(AcademicService s, ServiceRequest r) {
        return () -> s.submitRequest(r);
    }
    public static Callable<Optional<ServiceRequestSummary>> assignNextRequest(AcademicService s, String staffId) {
        return () -> s.assignNextRequest(staffId);
    }
}
