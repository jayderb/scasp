package com.scasp.report;

import com.scasp.model.ServiceRequest;
import java.util.Map;

/** Request workload summary: counts by status, by assignee (open work only) and by category. */
public record RequestWorkload(Map<ServiceRequest.Status, Long> byStatus, Map<String, Long> openByAssignee,
                             Map<String, Long> byCategory, double averageOpenPriority) {
    public RequestWorkload {
        byStatus = Map.copyOf(byStatus); openByAssignee = Map.copyOf(openByAssignee); byCategory = Map.copyOf(byCategory);
    }
}
