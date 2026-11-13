package com.scasp.web;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;

@WebServlet(name = "LoginServlet", urlPatterns = "/login")
public class LoginServlet extends BaseServlet {
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (user(req) != null && resp.getStatus() == 200 && req.getAttribute("error") == null) { resp.sendRedirect(req.getContextPath() + "/dashboard"); return; }
        HttpSession s = req.getSession(true);
        if (s.getAttribute("csrf") == null) s.setAttribute("csrf", AuthFilter.newToken());
        render(req, resp, "login");
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String userId = req.getParameter("username");
        String password = req.getParameter("password");
        if (userId == null || userId.isBlank() || password == null || password.isEmpty()) {
            resp.setStatus(400); req.setAttribute("error", "Enter your user id and password."); render(req, resp, "login"); return;
        }
        Optional<CurrentUser> user = repos().authenticator.authenticate(userId, password.toCharArray());
        if (user.isEmpty()) {
            resp.setStatus(401); req.setAttribute("error", "Invalid user id or password."); req.setAttribute("username", userId);
            render(req, resp, "login"); return;
        }
        req.changeSessionId();                       // session-fixation protection
        HttpSession s = req.getSession();
        s.setAttribute(CurrentUser.SESSION_KEY, user.get());
        s.setAttribute("csrf", AuthFilter.newToken());
        s.setMaxInactiveInterval(20 * 60);
        redirect(req, resp, "/dashboard", "Welcome, " + user.get().name() + ".");
    }
}
