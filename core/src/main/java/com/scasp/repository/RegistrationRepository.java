package com.scasp.repository;

import com.scasp.model.Registration;
import com.scasp.model.WaitlistEntry;
import java.util.List;
import java.util.Optional;

/** Registration persistence. Both mutators are single atomic transactions (registration + waiting list + audit). */
public interface RegistrationRepository {
    RegistrationOutcome register(String actor, String studentId, String courseCode);
    /** @return id of the student promoted from the waiting list, if any */
    Optional<String> withdraw(String actor, String studentId, String courseCode);
    List<Registration> findByStudent(String studentId);
    List<WaitlistEntry> waitingList(String courseCode);
    int enrolledCount(String courseCode);
}
