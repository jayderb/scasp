package com.scasp.repository;

import com.scasp.model.Student;
import java.util.List;
import java.util.Optional;

public interface StudentRepository {
    /** Inserts app_user + students rows in one transaction. */
    void create(Student student, String passwordHash);
    Optional<Student> findById(String studentId);
    List<Student> findAll();
    void update(Student student);
    boolean delete(String studentId);
}
