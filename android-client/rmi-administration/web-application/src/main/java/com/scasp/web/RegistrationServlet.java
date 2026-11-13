package com.scasp.web;

import com.scasp.enterprise.RegistrationService;
import jakarta.ejb.EJB;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Optional;

/**
 * GET /register: the student's registrations (staff: pick a student). POST /register: action=register|withdraw.
 * State changes go through the RegistrationBean EJB (container-managed transaction) then redirect-after-POST.
 */
@WebServlet(name = "RegistrationServlet", urlPatterns = "/register")
public class RegistrationServlet extends BaseServlet {
    @EJB private RegistrationService registrationService;

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser u = user(req);
        String studentId = u.isStaff() ? req.getParameter("studentId") : u.userId();
        if (studentId != null && !Validation.isId(studentId)) { resp.sendError(400, "Invalid student id"); return; }
        if (studentId != null && !studentId.isBlank()) {
            studentId = studentId.trim().toUpperCase();
            req.setAttribute("studentId", studentId);
            req.setAttribute("registrations", repos().registrations.findByStudent(studentId));
        }
        if (u.isStaff()) req.setAttribute("students", repos().students.findAll());
        req.setAttribute("courses", repos().courses.search("", true));
        render(req, resp, "registration");
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        CurrentUser u = user(req);
        Validation.Errors errors = new Validation.Errors();
        String code = errors.code("courseCode", req.getParameter("courseCode"));
        String studentId = u.isStaff() ? errors.id("studentId", req.getParameter("studentId")) : u.userId();
        String action = String.valueOf(req.getParameter("action"));
        if (!action.equals("register") && !action.equals("withdraw")) errors.add("action", "Unknown action");
        if (errors.any()) { renderWithStatus(req, resp, 400, "registration", "Please correct the highlighted fields.", errors.asMap()); return; }
        String back = "/register" + (u.isStaff() ? "?studentId=" + studentId : "");
        try {
            if (action.equals("register")) {
                String result = registrationService.register(u.userId(), studentId, code);
                redirect(req, resp, back, result.equals("REGISTERED") ? studentId + " is registered for " + code + "."
                        : code + " is full: " + studentId + " was added to the waiting list.");
            } else {
                Optional<String> promoted = registrationService.withdraw(u.userId(), studentId, code);
                redirect(req, resp, back, studentId + " withdrawn from " + code + "." + promoted.map(p -> " " + p + " was promoted from the waiting list.").orElse(""));
            }
        } catch (RuntimeException e) {
            if (!u.isStaff()) req.setAttribute("studentId", studentId);
            handleFailure(req, resp, "registration", e);
        }
    }
}
