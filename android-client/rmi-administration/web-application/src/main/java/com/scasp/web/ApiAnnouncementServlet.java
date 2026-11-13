package com.scasp.web;

import com.scasp.model.Announcement;
import com.scasp.model.Role;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

/**
 * GET /api/announcements : the "authorised resource" read by the Stage 3 HttpResourceFetcher (and the Android
 * client later). Allowed with a logged-in session, or with "Authorization: Bearer &lt;token&gt;" where the token is
 * configured on the server through the SCASP_API_TOKEN environment variable (never committed). Otherwise 401.
 */
@WebServlet(name = "ApiAnnouncementServlet", urlPatterns = "/api/announcements")
public class ApiAnnouncementServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        CurrentUser u = user(req);
        Role role = u != null ? u.role() : tokenValid(req) ? Role.STUDENT : null;
        if (role == null) {
            resp.setHeader("WWW-Authenticate", "Bearer realm=\"scasp\"");
            resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Sign in or provide a valid API token");
            return;
        }
        resp.setContentType("text/plain;charset=UTF-8");
        try (PrintWriter out = resp.getWriter()) {
            for (Announcement a : repos().announcements.findVisibleTo(role))
                out.println(a.announcementId() + "|" + a.audience() + "|" + a.publishedAt() + "|" + a.title().replace('|', ' '));
        }
    }
    private static boolean tokenValid(HttpServletRequest req) {
        String configured = System.getenv("SCASP_API_TOKEN");
        String header = req.getHeader("Authorization");
        if (configured == null || configured.isBlank() || header == null || !header.startsWith("Bearer ")) return false;
        return MessageDigest.isEqual(configured.getBytes(StandardCharsets.UTF_8), header.substring(7).getBytes(StandardCharsets.UTF_8));
    }
}
