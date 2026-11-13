package com.scasp.web;

import com.scasp.enterprise.RegistrationRuleException;
import com.scasp.enterprise.RegistrationService;
import com.scasp.model.CampusService;
import com.scasp.model.Course;
import com.scasp.model.Registration;
import com.scasp.model.ServiceRequestSummary;
import com.scasp.model.WaitlistEntry;
import com.scasp.repository.DataAccessException;
import com.scasp.service.AuthorisationException;
import jakarta.ejb.EJB;
import jakarta.ejb.EJBException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JSON API used by the Android client (Stage 10). Session-cookie authentication, same rules as the web pages:
 * <ul>
 *   <li>POST /api/login (form: userId, password; header X-SCASP-Client required) returns the user and a CSRF token.</li>
 *   <li>Every other endpoint needs a logged-in session (401 JSON otherwise).</li>
 *   <li>Every state-changing POST needs header X-CSRF-Token equal to the session token (403 otherwise).
 *       Browsers cannot add custom headers cross-site without CORS, so a forged cross-site form cannot call the API.</li>
 *   <li>Students only see and change their own data; ?studentId= is honoured for staff only.</li>
 * </ul>
 * Endpoints: /api/login, /api/logout, /api/me, /api/courses, /api/registrations, /api/requests, /api/campus-services.
 */
@WebServlet(name = "MobileApiServlet", urlPatterns = {"/api/login", "/api/logout", "/api/me", "/api/courses",
        "/api/registrations", "/api/requests", "/api/campus-services"})
public class MobileApiServlet extends BaseServlet {
    private static final Logger LOG = Logger.getLogger(MobileApiServlet.class.getName());
    static final String CLIENT_HEADER = "X-SCASP-Client";
    static final String CSRF_HEADER = "X-CSRF-Token";

    @EJB private RegistrationService registrationService;

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        CurrentUser u = user(req);
        if (u == null) { error(resp, 401, "Sign in first"); return; }
        try {
            switch (path) {
                case "/api/me" -> send(resp, 200, userJson(u, req.getSession(false)));
                case "/api/courses" -> courses(req, resp);
                case "/api/registrations" -> {
                    Optional<String> sid = targetStudent(req, resp, u);
                    if (sid.isPresent()) send(resp, 200, registrationsJson(sid.get()));
                }
                case "/api/requests" -> {
                    List<ServiceRequestSummary> list = u.isStaff() ? repos().requests.findOpenByPriority() : repos().requests.findByStudent(u.userId());
                    send(resp, 200, Json.obj("requests", list.stream().map(MobileApiServlet::requestJson).toList()));
                }
                case "/api/campus-services" -> send(resp, 200, Json.obj(
                        "note", "Coordinates are illustrative until the group replaces them with verified campus locations.",
                        "services", repos().campusServices.findAll().stream().map(MobileApiServlet::serviceJson).toList()));
                default -> error(resp, 405, "Use POST for " + path);
            }
        } catch (RuntimeException e) { failure(req, resp, e); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String path = req.getServletPath();
        if (path.equals("/api/login")) { login(req, resp); return; }
        CurrentUser u = user(req);
        if (u == null) { error(resp, 401, "Sign in first"); return; }
        if (!csrfOk(req)) { error(resp, 403, "Missing or invalid " + CSRF_HEADER + " header"); return; }
        try {
            switch (path) {
                case "/api/logout" -> { req.getSession().invalidate(); send(resp, 200, Json.obj("status", "signed out")); }
                case "/api/registrations" -> changeRegistration(req, resp, u);
                case "/api/requests" -> submitRequest(req, resp, u);
                default -> error(resp, 405, "Use GET for " + path);
            }
        } catch (RuntimeException e) { failure(req, resp, e); }
    }

    private void login(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (req.getHeader(CLIENT_HEADER) == null) { error(resp, 403, "Missing " + CLIENT_HEADER + " header"); return; }
        String userId = req.getParameter("userId"), password = req.getParameter("password");
        if (userId == null || userId.isBlank() || password == null || password.isEmpty()) { error(resp, 400, "Enter your user id and password."); return; }
        Optional<CurrentUser> user = repos().authenticator.authenticate(userId, password.toCharArray());
        if (user.isEmpty()) { error(resp, 401, "Invalid user id or password."); return; }
        HttpSession old = req.getSession(false);
        if (old != null) req.changeSessionId();     // session-fixation protection
        HttpSession s = req.getSession(true);
        s.setAttribute(CurrentUser.SESSION_KEY, user.get());
        s.setAttribute("csrf", AuthFilter.newToken());
        s.setMaxInactiveInterval(20 * 60);
        send(resp, 200, userJson(user.get(), s));
    }

    private void courses(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String q = req.getParameter("q");
        if (q != null && q.length() > 50) { error(resp, 400, "Search text is too long"); return; }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : repos().courses.search(q == null ? "" : q.trim(), true))
            rows.add(Json.obj("courseCode", c.getCourseCode(), "title", c.getTitle(), "capacity", c.getCapacity(),
                    "enrolled", repos().registrations.enrolledCount(c.getCourseCode()),
                    "waiting", repos().registrations.waitingList(c.getCourseCode()).size(), "active", c.isActive()));
        send(resp, 200, Json.obj("courses", rows));
    }

    private Map<String, Object> registrationsJson(String studentId) {
        List<Map<String, Object>> regs = new ArrayList<>(), waiting = new ArrayList<>();
        for (Registration r : repos().registrations.findByStudent(studentId))
            if (r.status() == Registration.Status.ACTIVE) regs.add(Json.obj("courseCode", r.courseCode(), "status", "REGISTERED", "since", r.createdAt()));
        for (Course c : repos().courses.search("", true))
            for (WaitlistEntry w : repos().registrations.waitingList(c.getCourseCode()))
                if (w.studentId().equals(studentId)) waiting.add(Json.obj("courseCode", c.getCourseCode(), "status", "WAITLISTED", "position", w.position()));
        return Json.obj("studentId", studentId, "registrations", regs, "waiting", waiting);
    }

    private void changeRegistration(HttpServletRequest req, HttpServletResponse resp, CurrentUser u) throws IOException {
        Validation.Errors errors = new Validation.Errors();
        String code = errors.code("courseCode", req.getParameter("courseCode"));
        String studentId = u.isStaff() ? errors.id("studentId", req.getParameter("studentId")) : u.userId();
        String action = String.valueOf(req.getParameter("action"));
        if (!action.equals("register") && !action.equals("withdraw")) errors.add("action", "Use register or withdraw");
        if (errors.any()) { send(resp, 400, Json.obj("error", "Please correct the highlighted fields.", "fieldErrors", errors.asMap())); return; }
        if (action.equals("register")) {
            String result = registrationService.register(u.userId(), studentId, code);
            send(resp, 200, Json.obj("result", result, "message", result.equals("REGISTERED")
                    ? studentId + " is registered for " + code + "." : code + " is full: " + studentId + " was added to the waiting list."));
        } else {
            Optional<String> promoted = registrationService.withdraw(u.userId(), studentId, code);
            send(resp, 200, Json.obj("result", "WITHDRAWN", "promoted", promoted.orElse(null),
                    "message", studentId + " withdrawn from " + code + "." + promoted.map(p -> " " + p + " was promoted from the waiting list.").orElse("")));
        }
    }

    private void submitRequest(HttpServletRequest req, HttpServletResponse resp, CurrentUser u) throws IOException {
        if (!u.isStudent()) { error(resp, 403, "Only students submit service requests"); return; }
        Validation.Errors errors = new Validation.Errors();
        String category = String.valueOf(req.getParameter("category"));
        if (!RequestServlet.CATEGORIES.contains(category)) errors.add("category", "Choose one of " + RequestServlet.CATEGORIES);
        String description = errors.text("description", req.getParameter("description"), 5, 1000);
        int priority = errors.integer("priority", req.getParameter("priority"), 1, 5);
        if (errors.any()) { send(resp, 400, Json.obj("error", "Please correct the highlighted fields.", "fieldErrors", errors.asMap())); return; }
        long id = repos().requests.create(u.userId(), u.userId(), category, description, priority);
        send(resp, 201, Json.obj("requestId", String.valueOf(id), "message", "Request #" + id + " submitted."));
    }

    /** Students always get their own id; staff may pass ?studentId=. */
    private Optional<String> targetStudent(HttpServletRequest req, HttpServletResponse resp, CurrentUser u) throws IOException {
        if (!u.isStaff()) return Optional.of(u.userId());
        String sid = req.getParameter("studentId");
        if (!Validation.isId(sid)) { error(resp, 400, "Staff must pass ?studentId="); return Optional.empty(); }
        return Optional.of(sid.trim().toUpperCase());
    }

    static Map<String, Object> userJson(CurrentUser u, HttpSession s) {
        return Json.obj("userId", u.userId(), "name", u.name(), "role", u.role(), "staff", u.isStaff(),
                "csrfToken", s == null ? null : s.getAttribute("csrf"));
    }
    static Map<String, Object> requestJson(ServiceRequestSummary r) {
        return Json.obj("requestId", r.requestId(), "studentId", r.studentId(), "category", r.category(), "description", r.description(),
                "priority", r.priority(), "status", r.status(), "assignedTo", r.assignedTo(), "createdAt", r.createdAt());
    }
    static Map<String, Object> serviceJson(CampusService c) {
        return Json.obj("serviceId", c.serviceId(), "name", c.name(), "description", c.description(),
                "latitude", c.latitude(), "longitude", c.longitude(), "illustrative", true);
    }

    private static boolean csrfOk(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        Object expected = s == null ? null : s.getAttribute("csrf");
        String actual = req.getHeader(CSRF_HEADER);
        return expected != null && actual != null
                && MessageDigest.isEqual(expected.toString().getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private void failure(HttpServletRequest req, HttpServletResponse resp, RuntimeException e) throws IOException {
        RuntimeException cause = e;
        if (e instanceof EJBException ejb && ejb.getCausedByException() instanceof RuntimeException r) cause = r;
        if (cause instanceof AuthorisationException) error(resp, 403, cause.getMessage());
        else if (cause instanceof RegistrationRuleException rr) error(resp, rr.isNotFound() ? 404 : 409, rr.getMessage());
        else if (cause instanceof IllegalArgumentException) error(resp, 404, cause.getMessage());
        else if (cause instanceof IllegalStateException) error(resp, 409, cause.getMessage());
        else if (cause instanceof DataAccessException dae && dae.isConstraintViolation()) error(resp, 409, "That change conflicts with existing data.");
        else {
            LOG.log(Level.SEVERE, "API request " + req.getAttribute("requestId") + " failed", e);
            error(resp, 500, "The request could not be completed. Reference " + req.getAttribute("requestId"));
        }
    }
    static void error(HttpServletResponse resp, int status, String message) throws IOException { send(resp, status, Json.obj("error", message)); }
    static void send(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json;charset=UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        resp.getWriter().write(Json.write(body));
    }
}
