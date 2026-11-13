package com.scasp.repository;

import com.scasp.model.ServiceRequest;
import com.scasp.model.ServiceRequestSummary;
import java.util.List;
import java.util.Optional;

public interface ServiceRequestRepository {
    /** @return the generated request id */
    long create(String actor, String studentId, String category, String description, int priority);
    Optional<ServiceRequestSummary> findById(long requestId);
    List<ServiceRequestSummary> findByStudent(String studentId);
    /** Open/unfinished requests, highest priority first, then oldest. */
    List<ServiceRequestSummary> findOpenByPriority();
    ServiceRequestSummary assign(String actor, long requestId, String staffId);
    ServiceRequestSummary updateStatus(String actor, long requestId, ServiceRequest.Status next);
}
