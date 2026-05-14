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
        /**
         * Active announcements, newest first.
         * SQL (when {@code deleted_at} exists): {@code SELECT * FROM ANNOUNCEMENT WHERE deleted_at IS NULL ORDER BY date_posted DESC}
         * Fallback (no column yet): {@code SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC}
         */
        @Override
        public List<Announcement> GetActive() {
            // Exact SQL when soft-delete column is present:
            String sqlWithSoftDelete = "SELECT * FROM ANNOUNCEMENT WHERE deleted_at IS NULL ORDER BY date_posted DESC";
            List<Announcement> announcements = new ArrayList<>();
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sqlWithSoftDelete)) {
                while (rs.next()) {
                    announcements.add(ResultSetToAnnouncement(rs));
                }
                return announcements;
            } catch (SQLException e) {
                if (isMissingDeletedAtColumn(e)) {
                    return queryAllAnnouncementsOrdered();
                }
                e.printStackTrace();
            }
            return announcements;
        }
        @Override
        public void SoftDelete(int announcementId) {
            String sql = "UPDATE ANNOUNCEMENT SET deleted_at = CURRENT_TIMESTAMP WHERE announcement_id = ? AND deleted_at IS NULL";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, announcementId);
                ps.executeUpdate();
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
        /**
         * Recent active announcements.
         * SQL (when {@code deleted_at} exists): {@code SELECT * FROM ANNOUNCEMENT WHERE deleted_at IS NULL ORDER BY date_posted DESC LIMIT ?}
         * Fallback: {@code SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC LIMIT ?}
         */
        @Override
        public List<Announcement> GetRecentAnnouncements(int limit) {
            // Exact SQL when soft-delete column is present:
            String sqlWithSoftDelete = "SELECT * FROM ANNOUNCEMENT WHERE deleted_at IS NULL ORDER BY date_posted DESC LIMIT ?";
            List<Announcement> announcements = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sqlWithSoftDelete)) {
                ps.setInt(1, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        announcements.add(ResultSetToAnnouncement(rs));
                    }
                }
                return announcements;
            } catch (SQLException e) {
                if (isMissingDeletedAtColumn(e)) {
                    String sqlLegacy = "SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC LIMIT ?";
                    try (PreparedStatement ps = conn.prepareStatement(sqlLegacy)) {
                        ps.setInt(1, limit);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                announcements.add(ResultSetToAnnouncement(rs));
                            }
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    e.printStackTrace();
                }
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
        private static boolean isMissingDeletedAtColumn(SQLException e) {
            String msg = e.getMessage();
            return msg != null && msg.contains("deleted_at")
                    && (msg.contains("Unknown column") || msg.contains("doesn't exist"));
        }
        /** All rows ordered by date when {@code deleted_at} is not available yet. */
        private List<Announcement> queryAllAnnouncementsOrdered() {
            List<Announcement> announcements = new ArrayList<>();
            String sqlLegacy = "SELECT * FROM ANNOUNCEMENT ORDER BY date_posted DESC";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sqlLegacy)) {
                while (rs.next()) {
                    announcements.add(ResultSetToAnnouncement(rs));
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return announcements;
        }
    }
