package com.scasp.persistence;

import com.scasp.model.Role;
import com.scasp.model.Student;
import com.scasp.repository.StudentRepository;
import com.scasp.repository.UserAccount;
import com.scasp.repository.UserAccountRepository;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/** Students (app_user + students tables) and login accounts. */
public class JdbcStudentRepository extends JdbcSupport implements StudentRepository, UserAccountRepository {
    private static final String SELECT = "SELECT u.user_id, u.full_name, u.active, s.programme, s.study_year "
            + "FROM students s JOIN app_user u ON u.user_id = s.student_id ";
    public JdbcStudentRepository(DataSource ds) { super(ds); }

    private static Student map(ResultSet rs) throws SQLException {
        Student s = new Student(rs.getString("user_id"), rs.getString("full_name"), rs.getString("programme"), rs.getInt("study_year"));
        s.setActive(rs.getBoolean("active"));
        return s;
    }
    @Override public void create(Student s, String passwordHash) {
        inTransaction(c -> {
            update(c, "INSERT INTO app_user (user_id, full_name, role, active, password_hash) VALUES (?, ?, 'STUDENT', ?, ?)",
                    s.getUserId(), s.getName(), s.isActive(), passwordHash);
            update(c, "INSERT INTO students (student_id, programme, study_year) VALUES (?, ?, ?)",
                    s.getUserId(), s.getProgramme(), s.getYearOfStudy());
            audit(c, "SYSTEM", "ADD_USER", "USER", s.getUserId(), "STUDENT");
            return null;
        });
    }
    @Override public Optional<Student> findById(String id) {
        return query(SELECT + "WHERE s.student_id = ?", JdbcStudentRepository::map, id).stream().findFirst();
    }
    @Override public List<Student> findAll() { return query(SELECT + "ORDER BY u.full_name, u.user_id", JdbcStudentRepository::map); }
    @Override public void update(Student s) {
        inTransaction(c -> {
            int n = update(c, "UPDATE app_user SET full_name = ?, active = ? WHERE user_id = ?", s.getName(), s.isActive(), s.getUserId());
            update(c, "UPDATE students SET programme = ?, study_year = ? WHERE student_id = ?", s.getProgramme(), s.getYearOfStudy(), s.getUserId());
            if (n == 0) throw new IllegalArgumentException("Unknown student");
            return null;
        });
    }
    /** students row is removed by ON DELETE CASCADE; fails if registrations reference the student. */
    @Override public boolean delete(String id) {
        return inTransaction(c -> update(c, "DELETE FROM app_user WHERE user_id = ? AND role = 'STUDENT'", id) == 1);
    }
    @Override public Optional<UserAccount> findAccount(String userId) {
        return query("SELECT user_id, full_name, role, active, password_hash FROM app_user WHERE user_id = ?",
                rs -> new UserAccount(rs.getString(1), rs.getString(2), Role.valueOf(rs.getString(3)), rs.getBoolean(4), rs.getString(5)),
                userId).stream().findFirst();
    }
    @Override public void updatePasswordHash(String userId, String hash) {
        inTransaction(c -> {
            if (update(c, "UPDATE app_user SET password_hash = ? WHERE user_id = ?", hash, userId) == 0) throw new IllegalArgumentException("Unknown user");
            audit(c, userId, "CHANGE_PASSWORD", "USER", userId, "");
            return null;
        });
    }
}
