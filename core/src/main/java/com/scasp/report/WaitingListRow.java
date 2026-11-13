package com.scasp.report;
import java.time.LocalDateTime;
public record WaitingListRow(String courseCode, String courseTitle, int position, String studentId, String fullName, LocalDateTime queuedAt) {}
