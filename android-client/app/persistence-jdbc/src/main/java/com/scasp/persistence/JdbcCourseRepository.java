package com.scasp.persistence;

import com.scasp.model.Course;
import com.scasp.repository.CourseRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class JdbcCourseRepository extends JdbcSupport implements CourseRepository {
    public JdbcCourseRepository(DataSource ds) { super(ds); }

    static Course map(ResultSet rs) throws SQLException {
        Course c = new Course(rs.getString("course_code"), rs.getString("title"), rs.getInt("capacity"));
        c.setActive(rs.getBoolean("active"));
        return c;
    }
    @Override public void create(Course course) {
        inTransaction(c -> {
            update(c, "INSERT INTO courses (course_code, title, capacity, active) VALUES (?, ?, ?, ?)",
                    course.getCourseCode(), course.getTitle(), course.getCapacity(), course.isActive());
            audit(c, "SYSTEM", "ADD_COURSE", "COURSE", course.getCourseCode(), course.getTitle());
            return null;
        });
    }
    public void create(String actor, Course course) {
        inTransaction(c -> {
            update(c, "INSERT INTO courses (course_code, title, capacity, active) VALUES (?, ?, ?, ?)",
                    course.getCourseCode(), course.getTitle(), course.getCapacity(), course.isActive());
            audit(c, actor, "ADD_COURSE", "COURSE", course.getCourseCode(), course.getTitle());
            return null;
        });
    }
    @Override public Optional<Course> findByCode(String code) {
        if (code == null) return Optional.empty();
        return query("SELECT course_code, title, capacity, active FROM courses WHERE course_code = ?", JdbcCourseRepository::map,
                code.trim().toUpperCase()).stream().findFirst();
    }
    @Override public List<Course> findAll() {
        return query("SELECT course_code, title, capacity, active FROM courses ORDER BY course_code", JdbcCourseRepository::map);
    }
    @Override public List<Course> search(String keyword, boolean activeOnly) {
        // The LIKE pattern is a bound parameter; wildcard characters typed by the user are escaped.
        String k = keyword == null ? "" : keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        String like = "%" + k + "%";
        return query("SELECT course_code, title, capacity, active FROM courses WHERE (course_code LIKE ? OR title LIKE ?)"
                + (activeOnly ? " AND active = TRUE" : "") + " ORDER BY course_code", JdbcCourseRepository::map, like, like);
    }
    @Override public void update(String actor, Course course) {
        inTransaction(c -> {
            List<Integer> enrolled = query(c, "SELECT COUNT(*) FROM registrations WHERE course_code = ? AND status = 'ACTIVE'",
                    rs -> rs.getInt(1), course.getCourseCode());
            if (course.getCapacity() < enrolled.get(0))
                throw new IllegalStateException("Capacity " + course.getCapacity() + " is below current enrolment " + enrolled.get(0));
            int n = update(c, "UPDATE courses SET title = ?, capacity = ? WHERE course_code = ?",
                    course.getTitle(), course.getCapacity(), course.getCourseCode());
            if (n == 0) throw new IllegalArgumentException("Unknown course");
            audit(c, actor, "UPDATE_COURSE", "COURSE", course.getCourseCode(), "title=" + course.getTitle() + ",capacity=" + course.getCapacity());
            return null;
        });
    }
    @Override public void setActive(String actor, String code, boolean active) {
        inTransaction(c -> {
            if (update(c, "UPDATE courses SET active = ? WHERE course_code = ?", active, code) == 0) throw new IllegalArgumentException("Unknown course");
            audit(c, actor, active ? "ACTIVATE_COURSE" : "DEACTIVATE_COURSE", "COURSE", code, "");
            return null;
        });
    }
    /** Fails with a constraint violation (DataAccessException) if registrations reference the course. */
    @Override public boolean delete(String code) {
        return inTransaction(c -> update(c, "DELETE FROM courses WHERE course_code = ?", code) == 1);
    }
}
