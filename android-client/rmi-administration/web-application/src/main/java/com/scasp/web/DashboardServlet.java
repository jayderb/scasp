package com.scasp.web;

import com.scasp.model.Announcement;
import com.scasp.model.ServiceRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/dashboard", "/announcements"})
public class DashboardServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser u = user(req);
        Repositories r = repos();
        req.setAttribute("announcements", r.announcements.findVisibleTo(u.role()));
        if (u.isStaff()) {
            req.setAttribute("capacity", r.reports.capacityReport());
            req.setAttribute("openRequests", r.requests.findOpenByPriority().size());
            req.setAttribute("audiences", Announcement.Audience.values());
        } else {
            req.setAttribute("registrations", r.registrations.findByStudent(u.userId()));
            req.setAttribute("myRequests", r.requests.findByStudent(u.userId()));
        }
        render(req, resp, "dashboard");
    }
    /** POST /announcements (staff): publish an announcement. */
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireStaff(req, resp)) return;
        Validation.Errors errors = new Validation.Errors();
        String title = errors.text("title", req.getParameter("title"), 3, 150);
        String body = errors.text("body", req.getParameter("body"), 3, 2000);
        Announcement.Audience audience = null;
        try { audience = Announcement.Audience.valueOf(String.valueOf(req.getParameter("audience"))); }
        catch (IllegalArgumentException e) { errors.add("audience", "Choose an audience"); }
        if (errors.any()) { renderWithStatus(req, resp, 400, "dashboard", "Please correct the highlighted fields.", errors.asMap()); return; }
        try {
            Announcement a = repos().announcements.publish(user(req).userId(), title, body, audience);
            redirect(req, resp, "/dashboard", "Announcement " + a.announcementId() + " published.");
        } catch (RuntimeException e) { handleFailure(req, resp, "dashboard", e); }
    }
    @SuppressWarnings("unused") private static final ServiceRequest.Status[] STATUSES = ServiceRequest.Status.values();
}
