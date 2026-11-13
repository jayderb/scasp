package com.scasp.web;

import com.scasp.enterprise.RegistrationRuleException;
import com.scasp.repository.DataAccessException;
import com.scasp.service.AuthorisationException;
import jakarta.ejb.EJBException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Shared controller helpers: current user, view forwarding, redirect-after-POST with flash messages, error mapping. */
public abstract class BaseServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    protected Repositories repos() { return (Repositories) getServletContext().getAttribute(Repositories.CONTEXT_KEY); }
    protected static CurrentUser user(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (CurrentUser) s.getAttribute(CurrentUser.SESSION_KEY);
    }
    /** Server-side forward to a JSP under /WEB-INF/views (not directly reachable by URL). */
    protected void render(HttpServletRequest req, HttpServletResponse resp, String view) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/views/" + view + ".jsp").forward(req, resp);
    }
    /** Re-render a form view with a status code (400/404/409) and field errors. */
    protected void renderWithStatus(HttpServletRequest req, HttpServletResponse resp, int status, String view, String error,
                                    Map<String, String> fieldErrors) throws ServletException, IOException {
        resp.setStatus(status);
        req.setAttribute("error", error);
        if (fieldErrors != null) req.setAttribute("fieldErrors", fieldErrors);
        doGet(req, resp);
    }
    /** Redirect-after-POST (PRG) with a one-time flash message shown by the messages fragment. */
    protected void redirect(HttpServletRequest req, HttpServletResponse resp, String path, String flash) throws IOException {
        if (flash != null) req.getSession().setAttribute("flash", flash);
        resp.sendRedirect(req.getContextPath() + path);
    }
    protected boolean requireStaff(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser u = user(req);
        if (u == null || !u.isStaff()) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Staff role required"); return false; }
        return true;
    }
    /** Maps business exceptions to HTTP status codes with user-safe messages. Returns true if handled. */
    protected void handleFailure(HttpServletRequest req, HttpServletResponse resp, String view, RuntimeException e) throws ServletException, IOException {
        RuntimeException cause = e;
        if (e instanceof EJBException ejb && ejb.getCausedByException() instanceof RuntimeException r) cause = r;
        if (cause instanceof AuthorisationException) { resp.sendError(HttpServletResponse.SC_FORBIDDEN, cause.getMessage()); return; }
        if (cause instanceof RegistrationRuleException rr) {
            renderWithStatus(req, resp, rr.isNotFound() ? 404 : 409, view, rr.getMessage(), null); return;
        }
        if (cause instanceof IllegalArgumentException) { renderWithStatus(req, resp, 404, view, cause.getMessage(), null); return; }
        if (cause instanceof IllegalStateException) { renderWithStatus(req, resp, 409, view, cause.getMessage(), null); return; }
        if (cause instanceof DataAccessException dae && dae.isConstraintViolation()) {
            renderWithStatus(req, resp, 409, view, "That change conflicts with existing data.", null); return;
        }
        LOG.log(Level.SEVERE, "Request " + req.getAttribute("requestId") + " failed", e);
        resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "The request could not be completed. Reference " + req.getAttribute("requestId"));
    }
}
