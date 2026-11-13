package com.scasp.enterprise;

import jakarta.ejb.Local;
import java.util.Optional;

/** Local business view of the registration enterprise bean (no-remote; used by the web tier in the same application). */
@Local
public interface RegistrationService {
    /** @return REGISTERED or WAITLISTED */
    String register(String actor, String studentId, String courseCode);
    /** @return the promoted student id, if any */
    Optional<String> withdraw(String actor, String studentId, String courseCode);
}
