package com.scasp.persistence;

import com.scasp.model.Announcement;
import com.scasp.model.Role;
import com.scasp.repository.AnnouncementRepository;

import javax.sql.DataSource;
import java.util.List;

public class JdbcAnnouncementRepository extends JdbcSupport implements AnnouncementRepository {
    public JdbcAnnouncementRepository(DataSource ds) { super(ds); }

    @Override public Announcement publish(String actor, String title, String body, Announcement.Audience audience) {
        if (title == null || title.isBlank() || body == null || body.isBlank()) throw new IllegalArgumentException("Title and body are required");
        return inTransaction(c -> {
            long id = insertReturningKey(c, "INSERT INTO announcement (title, body, audience, published_by) VALUES (?, ?, ?, ?)",
                    title.trim(), body.trim(), audience.name(), actor);
            audit(c, actor, "PUBLISH_ANNOUNCEMENT", "ANNOUNCEMENT", String.valueOf(id), audience.name());
            return query(c, "SELECT announcement_id, title, body, audience, published_at FROM announcement WHERE announcement_id = ?",
                    rs -> new Announcement(String.valueOf(rs.getLong(1)), rs.getString(2), rs.getString(3),
                            Announcement.Audience.valueOf(rs.getString(4)), ts(rs, "published_at")), id).get(0);
        });
    }
    @Override public List<Announcement> findVisibleTo(Role role) {
        String audience = role == Role.STUDENT ? "STUDENTS" : "STAFF";
        return query("SELECT announcement_id, title, body, audience, published_at FROM announcement WHERE audience IN ('ALL', ?) "
                        + "ORDER BY published_at DESC, announcement_id DESC",
                rs -> new Announcement(String.valueOf(rs.getLong(1)), rs.getString(2), rs.getString(3),
                        Announcement.Audience.valueOf(rs.getString(4)), ts(rs, "published_at")), audience);
    }
}
