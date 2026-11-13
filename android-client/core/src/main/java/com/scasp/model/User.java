package com.scasp.model;

import java.util.Objects;

/** Platform user. Identity (equals/hashCode) is the stable userId only. */
public class User {
    public enum Status { ACTIVE, SUSPENDED }

    private final String userId;
    private final String name;
    private final Role role;
    private volatile boolean active;

    public User(String userId, String name, Role role) {
        if (userId == null || userId.isBlank() || name == null || name.isBlank() || role == null)
            throw new IllegalArgumentException("User details are required");
        this.userId = userId.trim(); this.name = name.trim(); this.role = role; this.active = true;
    }
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public Role getRole() { return role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Status getStatus() { return active ? Status.ACTIVE : Status.SUSPENDED; }
    /** Staff and administrators may perform staff-only operations. */
    public boolean isStaff() { return role == Role.STAFF || role == Role.ADMIN; }
    @Override public boolean equals(Object o) { return o instanceof User u && userId.equals(u.userId); }
    @Override public int hashCode() { return Objects.hash(userId); }
    @Override public String toString() { return userId + " (" + name + ", " + role + ")"; }
}
