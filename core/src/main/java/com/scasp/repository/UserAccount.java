package com.scasp.repository;

import com.scasp.model.Role;

/** Login account as stored in app_user (password hash only, never the password). */
public record UserAccount(String userId, String fullName, Role role, boolean active, String passwordHash) {
    @Override public String toString() { return "UserAccount[" + userId + ", " + role + ", active=" + active + "]"; }
}
