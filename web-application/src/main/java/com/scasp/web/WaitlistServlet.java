package com.scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/** GET /waitlists[?course=CODE] (staff only - enforced by AuthFilter and re-checked here). */
@WebServlet(name = "WaitlistServlet", urlPatterns = "/waitlists")
public class WaitlistServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireStaff(req, resp)) return;
        String course = req.getParameter("course");
        if (course != null && !course.isBlank()) {
            if (!Validation.isCourseCode(course)) { resp.sendError(400, "Invalid course code"); return; }
            course = course.trim().toUpperCase();
            if (repos().courses.findByCode(course).isEmpty()) { resp.sendError(404, "Unknown course " + course); return; }
            req.setAttribute("course", course);
            req.setAttribute("entries", repos().registrations.waitingList(course));
        } else {
            req.setAttribute("rows", repos().reports.waitingListReport());
        }
        req.setAttribute("courses", repos().courses.findAll());
        render(req, resp, "waitlists");
    }
}
