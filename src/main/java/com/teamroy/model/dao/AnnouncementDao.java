package com.teamroy.model.dao;

import com.teamroy.model.entity.Announcement;
import java.util.List;
import java.time.LocalDateTime;

public interface AnnouncementDao extends GenericDao<Announcement> {

    // Useful for showing just the top X latest announcements on the dashboard
    List<Announcement> GetRecentAnnouncements(int limit);

    // Useful if the manager wants to filter announcements by a specific month/year
    List<Announcement> GetByDateRange(LocalDateTime start, LocalDateTime end);

    // Useful if you add a search bar later
    List<Announcement> SearchByTitle(String keyword);
}