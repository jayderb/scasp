package com.scasp.model;

import java.time.LocalDateTime;
import java.util.Objects;

/** Immutable announcement published to an audience. */
public record Announcement(String announcementId, String title, String body, Audience audience, LocalDateTime publishedAt) {
    /** Who an announcement is for. ALL is visible to every role. */
    public enum Audience {
        ALL, STUDENTS, STAFF;
        public boolean includes(Role role) {
            return this == ALL || (this == STUDENTS && role == Role.STUDENT) || (this == STAFF && role != Role.STUDENT);
        }
    }
    public Announcement {
        if (announcementId == null || announcementId.isBlank() || title == null || title.isBlank() || body == null || body.isBlank())
            throw new IllegalArgumentException("Announcement id, title and body are required");
        Objects.requireNonNull(audience, "audience"); Objects.requireNonNull(publishedAt, "publishedAt");
    }
}
