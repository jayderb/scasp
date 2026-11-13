package com.scasp.rmi;

import java.io.Serial;
import java.io.Serializable;

/** Serializable DTO: copied (pass-by-value) to the client; changing it on the client never affects the server. */
public record CourseSummaryDTO(String courseCode, String title, int capacity, int enrolled, int waiting, boolean active) implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
}
