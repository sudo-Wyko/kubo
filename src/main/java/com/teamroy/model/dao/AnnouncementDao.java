package com.teamroy.model.dao;
import com.teamroy.model.entity.Announcement;
import java.util.List;
import java.time.LocalDateTime;
public interface AnnouncementDao extends GenericDao<Announcement> {
    List<Announcement> GetRecentAnnouncements(int limit);
    List<Announcement> GetByDateRange(LocalDateTime start, LocalDateTime end);
    List<Announcement> SearchByTitle(String keyword);
}
