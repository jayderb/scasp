package com.scasp.web;

import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side input validation; field errors are shown next to the field by the JSP views. */
public final class Validation {
    private Validation() {}
    public static boolean isId(String s) { return s != null && s.trim().matches("[A-Za-z0-9_-]{2,30}"); }
    public static boolean isCourseCode(String s) { return s != null && s.trim().matches("[A-Za-z0-9_-]{2,20}"); }

    /** Collects field errors in insertion order. */
    public static final class Errors {
        private final Map<String, String> fields = new LinkedHashMap<>();
        public Errors add(String field, String message) { fields.putIfAbsent(field, message); return this; }
        public boolean any() { return !fields.isEmpty(); }
        public Map<String, String> asMap() { return Map.copyOf(fields); }
        public String text(String name, String value, int min, int max) {
            String v = value == null ? "" : value.trim();
            if (v.length() < min || v.length() > max) add(name, "Must be " + min + "-" + max + " characters");
            return v;
        }
        public int integer(String name, String value, int min, int max) {
            try {
                int n = Integer.parseInt(value == null ? "" : value.trim());
                if (n < min || n > max) { add(name, "Must be between " + min + " and " + max); return min; }
                return n;
            } catch (NumberFormatException e) { add(name, "Must be a whole number"); return min; }
        }
        public String code(String name, String value) {
            if (!isCourseCode(value)) { add(name, "Use 2-20 letters, digits, '-' or '_'"); return ""; }
            return value.trim().toUpperCase();
        }
        public String id(String name, String value) {
            if (!isId(value)) { add(name, "Use 2-30 letters, digits, '-' or '_'"); return ""; }
            return value.trim().toUpperCase();
        }
    }
}
