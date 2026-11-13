package com.scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /reports (staff only): the five JDBC join reports plus the recent audit trail. */
@WebServlet(name = "ReportServlet", urlPatterns = "/reports")
public class ReportServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireStaff(req, resp)) return;
        Repositories r = repos();
        req.setAttribute("capacity", r.reports.capacityReport());
        req.setAttribute("studentRegistrations", r.reports.studentRegistrationReport());
        req.setAttribute("waiting", r.reports.waitingListReport());
        req.setAttribute("workload", r.reports.requestWorkloadReport());
        req.setAttribute("auditByActor", r.reports.auditByActorReport());
        req.setAttribute("recentAudit", r.audit.findRecent(15));
        render(req, resp, "reports");
    }
}
