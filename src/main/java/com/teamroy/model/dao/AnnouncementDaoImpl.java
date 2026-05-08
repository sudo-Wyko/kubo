package com.teamroy.model.dao;

import com.teamroy.model.entity.Announcement;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;

public class AnnouncementDaoImpl implements AnnouncementDao {
    private Connection conn;

    public AnnouncementDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Announcement entity) {
        // Omitting date_posted in the INSERT so the database can use its DEFAULT
        // CURRENT_TIMESTAMP,
        // unless you specifically need to pass a manual date.
        String sql = "INSERT INTO ANNOUNCEMENT (title, message) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, entity.GetTitle());
            ps.setString(2, entity.GetMessage());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    entity.SetAnnouncementID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Announcement GetByID(int id) {
        String sql = "SELECT * FROM ANNOUNCEMENT WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToAnnouncement(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Announcement> GetAll() {
        List<Announcement> announcements = new ArrayList<>();
        String sql = "SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                announcements.add(ResultSetToAnnouncement(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return announcements;
    }

    @Override
    public void Update(Announcement entity) {
        String sql = "UPDATE ANNOUNCEMENT SET title=?, message=?, date_posted=? WHERE announcement_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entity.GetTitle());
            ps.setString(2, entity.GetMessage());
            ps.setObject(3, entity.GetDatePosted());
            ps.setInt(4, entity.GetAnnouncementID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int id) {
        String sql = "DELETE FROM ANNOUNCEMENT WHERE announcement_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Announcement> GetRecentAnnouncements(int limit) {
        List<Announcement> announcements = new ArrayList<>();
        String sql = "SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    announcements.add(ResultSetToAnnouncement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return announcements;
    }

    @Override
    public List<Announcement> GetByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Announcement> announcements = new ArrayList<>();
        String sql = "SELECT * FROM ANNOUNCEMENT WHERE date_posted BETWEEN ? AND ? ORDER BY date_posted DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    announcements.add(ResultSetToAnnouncement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return announcements;
    }

    @Override
    public List<Announcement> SearchByTitle(String keyword) {
        List<Announcement> announcements = new ArrayList<>();
        String sql = "SELECT * FROM ANNOUNCEMENT WHERE title LIKE ? ORDER BY date_posted DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    announcements.add(ResultSetToAnnouncement(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return announcements;
    }

    private Announcement ResultSetToAnnouncement(ResultSet rs) throws SQLException {
        Announcement a = new Announcement();
        a.SetAnnouncementID(rs.getInt("announcement_id"));
        a.SetTitle(rs.getString("title"));
        a.SetMessage(rs.getString("message"));
        a.SetDatePosted(rs.getObject("date_posted", LocalDateTime.class));
        return a;
    }
}