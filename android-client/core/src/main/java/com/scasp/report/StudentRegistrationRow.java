package com.scasp.report;
import java.time.LocalDateTime;
public record StudentRegistrationRow(String studentId, String fullName, String programme, String courseCode, String courseTitle, LocalDateTime registeredAt) {}
