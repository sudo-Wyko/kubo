package com.teamroy.model.dao;
import com.teamroy.model.entity.ActivityLog;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;
public class ActivityLogDaoImpl implements ActivityLogDao {
    private Connection conn;
    public ActivityLogDaoImpl(Connection conn) {
        this.conn = conn;
    }
    @Override
    public void Create(ActivityLog entity) {
        String sql = "INSERT INTO ACTIVITY_LOG (tenant_id, description) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.GetTenantID());
            ps.setString(2, entity.GetDescription());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    entity.SetActivityID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public ActivityLog GetByID(int id) {
        String sql = "SELECT * FROM ACTIVITY_LOG WHERE activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToActivityLog(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public List<ActivityLog> GetAll() {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ACTIVITY_LOG ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                logs.add(ResultSetToActivityLog(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    @Override
    public void Update(ActivityLog entity) {
        String sql = "UPDATE ACTIVITY_LOG SET tenant_id=?, description=?, created_at=? WHERE activity_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.GetTenantID());
            ps.setString(2, entity.GetDescription());
            ps.setObject(3, entity.GetCreatedAt());
            ps.setInt(4, entity.GetActivityID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void Delete(int id) {
        String sql = "DELETE FROM ACTIVITY_LOG WHERE activity_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    @Override
    public List<ActivityLog> GetByTenantID(int tenantId) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ACTIVITY_LOG WHERE tenant_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    logs.add(ResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    @Override
    public List<ActivityLog> GetRecentByTenantID(int tenantId, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ACTIVITY_LOG WHERE tenant_id = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    logs.add(ResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    @Override
    public List<ActivityLog> GetByDateRange(LocalDateTime start, LocalDateTime end) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM ACTIVITY_LOG WHERE created_at BETWEEN ? AND ? ORDER BY created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    logs.add(ResultSetToActivityLog(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }
    @Override
    public void DeleteByTenantID(int tenantId) {
        String sql = "DELETE FROM ACTIVITY_LOG WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private ActivityLog ResultSetToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog a = new ActivityLog();
        a.SetActivityID(rs.getInt("activity_id"));
        a.SetTenantID(rs.getInt("tenant_id"));
        a.SetDescription(rs.getString("description"));
        a.SetCreatedAt(rs.getObject("created_at", LocalDateTime.class));
        return a;
    }
}
