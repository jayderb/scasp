package com.scasp.report;

/** One row of the capacity report. */
public record CapacityRow(String courseCode, String title, int capacity, int enrolled, int waiting, boolean active) {
    public int seatsRemaining() { return Math.max(0, capacity - enrolled); }
    public double utilisationPercent() { return capacity == 0 ? 0 : (enrolled * 100.0) / capacity; }
}
