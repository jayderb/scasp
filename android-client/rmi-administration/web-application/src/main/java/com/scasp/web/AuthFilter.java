package com.scasp.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * Authentication, role and CSRF filter (mapped after the encoding filter in web.xml).
 * <ul>
 *   <li>Public paths: /login, /style.css, /api/* (the API servlet does its own token/session check).</li>
 *   <li>No session user: GET is redirected to /login, other methods get 401.</li>
 *   <li>Staff-only paths (/reports, /waitlists): STUDENT gets 403.</li>
 *   <li>Every POST must carry the per-session CSRF token, otherwise 403.</li>
 * </ul>
 */
public class AuthFilter implements Filter {
    private static final Set<String> PUBLIC = Set.of("/login", "/style.css", "/favicon.ico");
    private static final Set<String> STAFF_ONLY = Set.of("/reports", "/waitlists");
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest rq = (HttpServletRequest) req; HttpServletResponse rs = (HttpServletResponse) res;
        String path = rq.getRequestURI().substring(rq.getContextPath().length());
        HttpSession session = rq.getSession(false);
        if (session != null && session.getAttribute("csrf") == null) session.setAttribute("csrf", newToken());

        if (PUBLIC.contains(path) || path.startsWith("/api/")) {
            if (path.equals("/login") && "POST".equals(rq.getMethod()) && !csrfOk(rq, session)) { forbidden(rq, rs, "Form expired, please try again."); return; }
            chain.doFilter(req, res); return;
        }
        CurrentUser user = session == null ? null : (CurrentUser) session.getAttribute(CurrentUser.SESSION_KEY);
        if (user == null) {
            if ("GET".equals(rq.getMethod())) rs.sendRedirect(rq.getContextPath() + "/login");
            else rs.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in");
            return;
        }
        if (STAFF_ONLY.contains(path) && !user.isStaff()) { forbidden(rq, rs, "This page is for staff only."); return; }
        if ("POST".equals(rq.getMethod()) && !csrfOk(rq, session)) { forbidden(rq, rs, "Form expired, please try again."); return; }
        rs.setHeader("Cache-Control", "no-store");   // authenticated pages are never cached
        chain.doFilter(req, res);
    }
    private static boolean csrfOk(HttpServletRequest rq, HttpSession session) {
        if (session == null) return false;
        Object expected = session.getAttribute("csrf");
        String actual = rq.getParameter("csrf");
        return expected != null && actual != null && java.security.MessageDigest.isEqual(expected.toString().getBytes(), actual.getBytes());
    }
    private static void forbidden(HttpServletRequest rq, HttpServletResponse rs, String msg) throws IOException {
        rs.sendError(HttpServletResponse.SC_FORBIDDEN, msg);
    }
    static String newToken() { byte[] b = new byte[24]; RANDOM.nextBytes(b); return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
}
