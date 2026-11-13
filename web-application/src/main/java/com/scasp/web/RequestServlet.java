package com.scasp.web;

import com.scasp.model.ServiceRequest;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * GET /requests: students see their own requests and a submission form; staff see the priority queue.
 * POST /requests: action=submit (student) | assign | status (staff).
 */
@WebServlet(name = "RequestServlet", urlPatterns = "/requests")
public class RequestServlet extends BaseServlet {
    static final List<String> CATEGORIES = List.of("TRANSCRIPT", "CLEARANCE", "APPEAL", "REGISTRATION", "OTHER");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser u = user(req);
        req.setAttribute("categories", CATEGORIES);
        if (u.isStaff()) {
            req.setAttribute("queue", repos().requests.findOpenByPriority());
            req.setAttribute("statuses", List.of(ServiceRequest.Status.IN_PROGRESS, ServiceRequest.Status.RESOLVED, ServiceRequest.Status.REJECTED));
        } else {
            req.setAttribute("myRequests", repos().requests.findByStudent(u.userId()));
        }
        render(req, resp, "requests");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser u = user(req);
        String action = String.valueOf(req.getParameter("action"));
        Validation.Errors errors = new Validation.Errors();
        try {
            switch (action) {
                case "submit" -> {
                    if (!u.isStudent()) { resp.sendError(403, "Only students submit requests"); return; }
                    String category = String.valueOf(req.getParameter("category"));
                    if (!CATEGORIES.contains(category)) errors.add("category", "Choose a category");
                    String description = errors.text("description", req.getParameter("description"), 5, 1000);
                    int priority = errors.integer("priority", req.getParameter("priority"), 1, 5);
                    if (errors.any()) { renderWithStatus(req, resp, 400, "requests", "Please correct the highlighted fields.", errors.asMap()); return; }
                    long id = repos().requests.create(u.userId(), u.userId(), category, description, priority);
                    redirect(req, resp, "/requests", "Request #" + id + " submitted.");
                }
                case "assign", "status" -> {
                    if (!requireStaff(req, resp)) return;
                    long id;
                    try { id = Long.parseLong(String.valueOf(req.getParameter("requestId"))); }
                    catch (NumberFormatException e) { resp.sendError(400, "Invalid request id"); return; }
                    if (action.equals("assign")) {
                        String staff = req.getParameter("staffId") == null || req.getParameter("staffId").isBlank() ? u.userId() : errors.id("staffId", req.getParameter("staffId"));
                        if (errors.any()) { renderWithStatus(req, resp, 400, "requests", "Invalid staff id.", errors.asMap()); return; }
                        repos().requests.assign(u.userId(), id, staff);
                        redirect(req, resp, "/requests", "Request #" + id + " assigned to " + staff + ".");
                    } else {
                        ServiceRequest.Status next;
                        try { next = ServiceRequest.Status.valueOf(String.valueOf(req.getParameter("status"))); }
                        catch (IllegalArgumentException e) { resp.sendError(400, "Invalid status"); return; }
                        repos().requests.updateStatus(u.userId(), id, next);
                        redirect(req, resp, "/requests", "Request #" + id + " is now " + next + ".");
                    }
                }
                default -> resp.sendError(400, "Unknown action");
            }
        } catch (RuntimeException e) { handleFailure(req, resp, "requests", e); }
    }
}
