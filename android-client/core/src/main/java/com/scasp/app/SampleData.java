package com.scasp.app;

import com.scasp.model.*;
import com.scasp.service.AcademicService;

/** Fictional sample data shared by the demo, TCP gateway and RMI server. No real student records. */
public final class SampleData {
    private SampleData() {}
    public static AcademicService load(AcademicService s) {
        s.addStudent(new Student("STU001", "Alex Banda", "Computer Science", 2));
        s.addStudent(new Student("STU002", "Mary Phiri", "Computer Science", 2));
        s.addStudent(new Student("STU003", "John Zulu", "Information Technology", 1));
        s.addStudent(new Student("STU004", "Ruth Mwale", "Information Technology", 3));
        s.addUser(new User("STAFF01", "Grace Tembo", Role.STAFF));
        s.addUser(new User("ADMIN01", "Peter Lungu", Role.ADMIN));
        s.addCourse(new Course("CSC301", "Advanced Java", 2));
        s.addCourse(new Course("DB302", "Advanced Database", 3));
        s.addCourse(new Course("HCI308", "Human Computer Interface", 2));
        return s;
    }
}
