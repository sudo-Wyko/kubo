package com.teamroy.model.dao;

import com.teamroy.model.entity.MaintenanceRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;

public class MaintenanceRequestDaoImpl implements MaintenanceRequestDao {
    private Connection conn;

    public MaintenanceRequestDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(MaintenanceRequest request) {
        String sql = "INSERT INTO MAINTENANCE_REQUEST (tenant_id, room_id, report_description, reported_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, request.GetTenantID());

            // Handle nullable RoomId
            if (request.GetRoomID() != null) {
                ps.setInt(2, request.GetRoomID());
            } else {
                ps.setNull(2, Types.INTEGER);
            }

            ps.setString(3, request.GetReportDescription());
            ps.setObject(4, request.GetReportedDate()); // LocalDateTime
            ps.setString(5, request.GetStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    request.SetRequestID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public MaintenanceRequest GetByID(int requestId) {
        String sql = "SELECT * FROM MAINTENANCE_REQUEST WHERE request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToRequest(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<MaintenanceRequest> GetAll() {
        List<MaintenanceRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM MAINTENANCE_REQUEST ORDER BY reported_date DESC";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                requests.add(ResultSetToRequest(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    @Override
    public void Update(MaintenanceRequest request) {
        String sql = "UPDATE MAINTENANCE_REQUEST SET tenant_id=?, room_id=?, report_description=?, reported_date=?, status=? WHERE request_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, request.GetTenantID());
            if (request.GetRoomID() != null)
                ps.setInt(2, request.GetRoomID());
            else
                ps.setNull(2, Types.INTEGER);

            ps.setString(3, request.GetReportDescription());
            ps.setObject(4, request.GetReportedDate());
            ps.setString(5, request.GetStatus());
            ps.setInt(6, request.GetRequestID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int requestId) {
        String sql = "DELETE FROM MAINTENANCE_REQUEST WHERE request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, requestId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<MaintenanceRequest> GetByStatus(String status) {
        List<MaintenanceRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM MAINTENANCE_REQUEST WHERE status = ? ORDER BY reported_date ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    requests.add(ResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return requests;
    }

    @Override
    public List<MaintenanceRequest> GetByTenantID(int tenantId) {
        List<MaintenanceRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM MAINTENANCE_REQUEST WHERE tenant_id = ? ORDER BY reported_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    requests.add(ResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    @Override
    public List<MaintenanceRequest> GetByRoomID(int roomId) {
        List<MaintenanceRequest> requests = new ArrayList<>();
        String sql = "SELECT * FROM MAINTENANCE_REQUEST WHERE room_id = ? ORDER BY reported_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    requests.add(ResultSetToRequest(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    @Override
    public boolean UpdateStatus(int requestId, String status) {
        String sql = "UPDATE MAINTENANCE_REQUEST SET status = ? WHERE request_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private MaintenanceRequest ResultSetToRequest(ResultSet rs) throws SQLException {
        MaintenanceRequest mr = new MaintenanceRequest();
        mr.SetRequestID(rs.getInt("request_id"));
        mr.SetTenantID(rs.getInt("tenant_id"));

        // Handle nullable Integer roomId from DB
        int roomId = rs.getInt("room_id");
        mr.SetRoomID(rs.wasNull() ? null : roomId);

        mr.SetReportDescription(rs.getString("report_description"));
        mr.SetReportedDate(rs.getObject("reported_date", LocalDateTime.class));
        mr.SetStatus(rs.getString("status"));
        return mr;
    }

}
