package com.scasp.repository;

import com.scasp.model.Course;
import java.util.List;
import java.util.Optional;

public interface CourseRepository {
    void create(Course course);
    Optional<Course> findByCode(String code);
    List<Course> findAll();
    List<Course> search(String keyword, boolean activeOnly);
    /** Updates title/capacity; capacity may not drop below current enrolment. */
    void update(String actor, Course course);
    void setActive(String actor, String code, boolean active);
    boolean delete(String code);
}
