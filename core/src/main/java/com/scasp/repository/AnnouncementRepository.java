package com.scasp.repository;

import com.scasp.model.Announcement;
import com.scasp.model.Role;
import java.util.List;

public interface AnnouncementRepository {
    Announcement publish(String actor, String title, String body, Announcement.Audience audience);
    List<Announcement> findVisibleTo(Role role);
}
