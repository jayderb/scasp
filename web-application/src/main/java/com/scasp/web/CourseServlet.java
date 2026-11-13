package com.scasp.web;

import com.scasp.model.Course;
import com.scasp.service.CourseComparators;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * GET /courses?q=&amp;sort= : search and sort the catalogue. The chosen sort order is remembered in a cookie
 * ("scasp_sort") - a justified, non-sensitive UI preference (no identity or session data in it).
 * POST /courses (staff): action=create|update|activate|deactivate.
 */
@WebServlet(name = "CourseServlet", urlPatterns = "/courses")
public class CourseServlet extends BaseServlet {
    private static final Set<String> SORTS = Set.of("code", "title", "capacity", "status");

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String sort = req.getParameter("sort");
        if (sort != null && SORTS.contains(sort)) {
            Cookie c = new Cookie("scasp_sort", sort);
            c.setHttpOnly(true); c.setMaxAge(30 * 24 * 3600); c.setPath(req.getContextPath().isEmpty() ? "/" : req.getContextPath());
            c.setAttribute("SameSite", "Lax");
            resp.addCookie(c);
        } else {
            sort = "code";
            if (req.getCookies() != null) for (Cookie c : req.getCookies())
                if (c.getName().equals("scasp_sort") && SORTS.contains(c.getValue())) sort = c.getValue();
        }
        String q = req.getParameter("q");
        if (q != null && q.length() > 50) q = q.substring(0, 50);
        List<Course> courses = repos().courses.search(q, !user(req).isStaff());
        Map<String, Integer> enrolled = new java.util.HashMap<>();
        repos().reports.capacityReport().forEach(r -> enrolled.put(r.courseCode(), r.enrolled()));
        req.setAttribute("courses", courses.stream().sorted(CourseComparators.forKey(sort)).toList());
        req.setAttribute("enrolled", enrolled);
        req.setAttribute("q", q == null ? "" : q);
        req.setAttribute("sort", sort);
        render(req, resp, "courses");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (!requireStaff(req, resp)) return;
        String action = String.valueOf(req.getParameter("action"));
        String actor = user(req).userId();
        Validation.Errors errors = new Validation.Errors();
        String code = errors.code("courseCode", req.getParameter("courseCode"));
        try {
            switch (action) {
                case "create", "update" -> {
                    String title = errors.text("title", req.getParameter("title"), 3, 120);
                    int capacity = errors.integer("capacity", req.getParameter("capacity"), 1, 1000);
                    if (errors.any()) { renderWithStatus(req, resp, 400, "courses", "Please correct the highlighted fields.", errors.asMap()); return; }
                    if (action.equals("create")) repos().courses.create(actor, new Course(code, title, capacity));
                    else repos().courses.update(actor, new Course(code, title, capacity));
                    redirect(req, resp, "/courses", "Course " + code + (action.equals("create") ? " created." : " updated."));
                }
                case "activate", "deactivate" -> {
                    if (errors.any()) { renderWithStatus(req, resp, 400, "courses", "Invalid course code.", errors.asMap()); return; }
                    repos().courses.setActive(actor, code, action.equals("activate"));
                    redirect(req, resp, "/courses", "Course " + code + " " + action + "d.");
                }
                default -> resp.sendError(400, "Unknown action");
            }
        } catch (RuntimeException e) { handleFailure(req, resp, "courses", e); }
    }
}
