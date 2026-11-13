package com.scasp.repository;

/** Result of a persisted registration; {@code generatedId} is the database key of the registration or waiting-list row. */
public record RegistrationOutcome(String status, long generatedId) {
    public static final String REGISTERED = "REGISTERED", WAITLISTED = "WAITLISTED";
}
