package com.scasp.web;

import com.scasp.model.Role;
import java.io.Serial;
import java.io.Serializable;

/** The authenticated user kept in the HttpSession (no password or hash). */
public record CurrentUser(String userId, String name, Role role) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    public static final String SESSION_KEY = "user";
    public boolean isStaff() { return role == Role.STAFF || role == Role.ADMIN; }
    public boolean isStudent() { return role == Role.STUDENT; }
    /** JavaBean-style accessors so EL (${sessionScope.user.staff}) works. */
    public boolean getStaff() { return isStaff(); }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public Role getRole() { return role; }
}
