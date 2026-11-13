package com.scasp.model;

import java.util.Objects;

/**
 * Course in the catalogue. Natural ordering and identity are both based on the course code,
 * so compareTo is consistent with equals (required for TreeSet/TreeMap correctness).
 * Code, title and capacity are immutable; updates create a new instance via {@link #withDetails}.
 */
public class Course implements Comparable<Course> {
    public enum Status { ACTIVE, INACTIVE }

    private final String courseCode, title;
    private final int capacity;
    private volatile boolean active = true;

    public Course(String code, String title, int capacity) {
        if (code == null || !code.trim().matches("[A-Za-z0-9_-]{2,20}") || title == null || title.isBlank() || capacity < 1)
            throw new IllegalArgumentException("Valid course code, title and capacity are required");
        this.courseCode = code.trim().toUpperCase(); this.title = title.trim(); this.capacity = capacity;
    }
    public String getCourseCode() { return courseCode; }
    public String getTitle() { return title; }
    public int getCapacity() { return capacity; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Status getStatus() { return active ? Status.ACTIVE : Status.INACTIVE; }
    /** Returns an updated copy with the same identity (course code) and status. */
    public Course withDetails(String newTitle, int newCapacity) {
        Course c = new Course(courseCode, newTitle, newCapacity);
        c.setActive(active);
        return c;
    }
    /** Defensive copy used at service boundaries so callers cannot change catalogue state. */
    public Course copy() { return withDetails(title, capacity); }
    @Override public int compareTo(Course other) { return courseCode.compareTo(other.courseCode); }
    @Override public boolean equals(Object o) { return o instanceof Course c && courseCode.equals(c.courseCode); }
    @Override public int hashCode() { return Objects.hash(courseCode); }
    @Override public String toString() { return courseCode + " - " + title + " (capacity " + capacity + ")"; }
}
