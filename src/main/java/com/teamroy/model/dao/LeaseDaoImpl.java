package com.teamroy.model.dao;

import com.teamroy.model.entity.Lease;
import java.time.LocalDate;
import java.util.*;
import java.sql.*;
import java.sql.Date;

public class LeaseDaoImpl implements LeaseDao {
    private Connection conn;

    public LeaseDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Lease lease) {
        String sql = "INSERT INTO LEASE (tenant_id, room_id, start_date, end_date, monthly_rent, status) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, lease.GetTenantID());
            ps.setInt(2, lease.GetRoomID());
            ps.setDate(3, Date.valueOf(lease.GetStartDate()));
            ps.setDate(4, Date.valueOf(lease.GetEndDate()));
            ps.setDouble(5, lease.GetMonthlyRent());
            ps.setString(6, lease.GetStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    lease.SetLeaseID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Lease GetByID(int leaseId) {
        String sql = "SELECT * FROM LEASE WHERE lease_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToLease(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Lease> GetAll() {
        List<Lease> leases = new ArrayList<>();
        String sql = "SELECT * FROM LEASE";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                leases.add(ResultSetToLease(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leases;
    }

    @Override
    public void Update(Lease lease) {
        String sql = "UPDATE LEASE SET tenant_id=?, room_id=?, start_date=?, end_date=?, monthly_rent=?, status=? WHERE lease_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lease.GetTenantID());
            ps.setInt(2, lease.GetRoomID());
            ps.setDate(3, Date.valueOf(lease.GetStartDate()));
            ps.setDate(4, Date.valueOf(lease.GetEndDate()));
            ps.setDouble(5, lease.GetMonthlyRent());
            ps.setString(6, lease.GetStatus());
            ps.setInt(7, lease.GetLeaseID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int leaseId) {
        String sql = "DELETE FROM LEASE WHERE lease_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Lease> GetByTenantID(int tenantId) {
        List<Lease> leases = new ArrayList<>();
        String sql = "SELECT * FROM LEASE WHERE tenant_id = ? ORDER BY start_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    leases.add(ResultSetToLease(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leases;
    }

    @Override
    public Lease GetActiveLeaseByRoom(int roomId) {
        String sql = "SELECT * FROM LEASE WHERE room_id = ? AND status = 'ACTIVE' LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToLease(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Lease> GetByStatus(String status) {
        List<Lease> leases = new ArrayList<>();
        String sql = "SELECT * FROM LEASE WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    leases.add(ResultSetToLease(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leases;
    }

    @Override
    public List<Lease> GetExpiringSoon(LocalDate endDateThreshold) {
        List<Lease> leases = new ArrayList<>();
        // Finds ACTIVE leases ending between today and the threshold date
        String sql = "SELECT * FROM LEASE WHERE status = 'ACTIVE' AND end_date <= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(endDateThreshold));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    leases.add(ResultSetToLease(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return leases;
    }

    @Override
    public boolean UpdateStatus(int leaseId, String status) {
        String sql = "UPDATE LEASE SET status = ? WHERE lease_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, leaseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean IsRoomAvailable(int roomId, LocalDate start, LocalDate end) {
        // Overlap logic: (StartA <= EndB) and (EndA >= StartB)
        String sql = "SELECT COUNT(*) FROM LEASE WHERE room_id = ? AND status = 'ACTIVE' " +
                "AND (start_date <= ? AND end_date >= ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setDate(2, Date.valueOf(end));
            ps.setDate(3, Date.valueOf(start));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 0; // Available if count is 0
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Lease ResultSetToLease(ResultSet rs) throws SQLException {
        Lease l = new Lease();
        l.SetLeaseID(rs.getInt("lease_id"));
        l.SetTenantID(rs.getInt("tenant_id"));
        l.SetRoomID(rs.getInt("room_id"));
        l.SetStartDate(rs.getDate("start_date").toLocalDate());
        l.SetEndDate(rs.getDate("end_date").toLocalDate());
        l.SetMonthlyRent(rs.getDouble("monthly_rent"));
        l.SetStatus(rs.getString("status"));
        return l;
    }

}
