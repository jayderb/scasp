package com.scasp.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * First filter in the chain (ordered in web.xml): UTF-8 for request/response, security headers, and a
 * correlation id that is logged with method/path/status/duration and exposed to error pages.
 */
public class EncodingAndLoggingFilter implements Filter {
    private static final Logger LOG = Logger.getLogger("scasp.http");
    @Override public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest rq = (HttpServletRequest) req; HttpServletResponse rs = (HttpServletResponse) res;
        rq.setCharacterEncoding("UTF-8");
        rs.setCharacterEncoding("UTF-8");
        String id = UUID.randomUUID().toString().substring(0, 8);
        rq.setAttribute("requestId", id);
        rs.setHeader("X-Request-Id", id);
        rs.setHeader("X-Content-Type-Options", "nosniff");
        rs.setHeader("X-Frame-Options", "DENY");
        rs.setHeader("Referrer-Policy", "same-origin");
        rs.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'");
        long start = System.nanoTime();
        try { chain.doFilter(req, res); }
        finally {
            Object user = rq.getSession(false) == null ? null : rq.getSession(false).getAttribute(CurrentUser.SESSION_KEY);
            LOG.info(String.format("[%s] %s %s -> %d (%d ms) user=%s", id, rq.getMethod(), rq.getRequestURI(), rs.getStatus(),
                    (System.nanoTime() - start) / 1_000_000, user instanceof CurrentUser u ? u.userId() : "-"));
        }
    }
}
