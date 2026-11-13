package com.scasp.service;

import com.scasp.model.Course;
import java.util.Comparator;
import java.util.Map;

/** Multiple sorting strategies for the catalogue (natural ordering = course code, see Course.compareTo). */
public final class CourseComparators {
    private CourseComparators() {}
    public static final Comparator<Course> BY_CODE = Comparator.naturalOrder();
    public static final Comparator<Course> BY_TITLE = Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER).thenComparing(BY_CODE);
    public static final Comparator<Course> BY_CAPACITY_DESC = Comparator.comparingInt(Course::getCapacity).reversed().thenComparing(BY_CODE);
    public static final Comparator<Course> ACTIVE_FIRST = Comparator.comparing(Course::isActive).reversed().thenComparing(BY_CODE);
    /** Most available seats first; needs the current enrolment counts. */
    public static Comparator<Course> bySeatsRemainingDesc(Map<String, Integer> enrolled) {
        return Comparator.<Course>comparingInt(c -> c.getCapacity() - enrolled.getOrDefault(c.getCourseCode(), 0)).reversed().thenComparing(BY_CODE);
    }
    /** Resolves a user-supplied sort key (e.g. from a web form) safely; unknown keys fall back to code. */
    public static Comparator<Course> forKey(String key) {
        if (key == null) return BY_CODE;
        return switch (key.toLowerCase()) {
            case "title" -> BY_TITLE;
            case "capacity" -> BY_CAPACITY_DESC;
            case "status" -> ACTIVE_FIRST;
            default -> BY_CODE;
        };
    }
}
