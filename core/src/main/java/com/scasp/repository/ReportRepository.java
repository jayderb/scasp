package com.scasp.repository;

import com.scasp.report.*;
import java.util.List;

/** Join-based reports (each query joins at least two tables). */
public interface ReportRepository {
    List<CapacityRow> capacityReport();
    List<StudentRegistrationRow> studentRegistrationReport();
    List<WaitingListRow> waitingListReport();
    List<WorkloadRow> requestWorkloadReport();
    List<AuditActorRow> auditByActorReport();
}
