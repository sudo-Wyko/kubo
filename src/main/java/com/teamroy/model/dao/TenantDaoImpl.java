package com.teamroy.model.dao;

import com.teamroy.model.entity.Tenant;
import java.util.*;
import java.sql.*;

public class TenantDaoImpl implements TenantDao {
    private Connection conn;

    public TenantDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Tenant tenant) {
        String sql = "INSERT INTO TENANT (user_id, first_name, last_name, contact_number, email, total_balance) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (tenant.GetUserID() != null)
                ps.setInt(1, tenant.GetUserID());
            else
                ps.setNull(1, Types.INTEGER);

            ps.setString(2, tenant.GetFirstName());
            ps.setString(3, tenant.GetLastName());
            ps.setString(4, tenant.GetContactNumber());
            ps.setString(5, tenant.GetEmail());
            ps.setDouble(6, tenant.GetTotalBalance());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    tenant.SetTenantID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Tenant GetByID(int tenantId) {
        String sql = "SELECT * FROM TENANT WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToTenant(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Tenant> GetAll() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM TENANT";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                tenants.add(ResultSetToTenant(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tenants;
    }

    @Override
    public Tenant GetByUserID(int userId) {
        String sql = "SELECT * FROM TENANT WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToTenant(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Tenant> GetByName(String name) {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM TENANT WHERE (first_name LIKE ? OR last_name LIKE ?) AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String searchPattern = "%" + name + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    tenants.add(ResultSetToTenant(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tenants;
    }

    @Override
    public List<Tenant> GetAllActive() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM TENANT WHERE deleted_at IS NULL";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                tenants.add(ResultSetToTenant(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tenants;
    }

    @Override
    public void Restore(int tenantId) {
        String sql = "UPDATE TENANT SET deleted_at = NULL WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Tenant> GetTenantsWithBalance() {
        List<Tenant> tenants = new ArrayList<>();
        String sql = "SELECT * FROM TENANT WHERE total_balance > 0 AND deleted_at IS NULL";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                tenants.add(ResultSetToTenant(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tenants;
    }

    @Override
    public boolean UpdateBalance(int tenantId, double amount) {
        // This adds the amount to the existing balance (amount can be negative for
        // payments)
        String sql = "UPDATE TENANT SET total_balance = total_balance + ? WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setInt(2, tenantId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void Update(Tenant tenant) {
        String sql = "UPDATE TENANT SET user_id=?, first_name=?, last_name=?, contact_number=?, email=?, total_balance=? WHERE tenant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (tenant.GetUserID() != null)
                ps.setInt(1, tenant.GetUserID());
            else
                ps.setNull(1, Types.INTEGER);

            ps.setString(2, tenant.GetFirstName());
            ps.setString(3, tenant.GetLastName());
            ps.setString(4, tenant.GetContactNumber());
            ps.setString(5, tenant.GetEmail());
            ps.setDouble(6, tenant.GetTotalBalance());
            ps.setInt(7, tenant.GetTenantID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int tenantId) {
        // Soft delete logic
        String sql = "UPDATE TENANT SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Tenant ResultSetToTenant(ResultSet rs) throws SQLException {
        Tenant t = new Tenant();
        t.SetTenantID(rs.getInt("tenant_id"));

        int userId = rs.getInt("user_id");
        t.SetUserID(rs.wasNull() ? null : userId);

        t.SetFirstName(rs.getString("first_name"));
        t.SetLastName(rs.getString("last_name"));
        t.SetContactNumber(rs.getString("contact_number"));
        t.SetEmail(rs.getString("email"));
        t.SetTotalBalance(rs.getDouble("total_balance"));
        t.SetTimeDeletedAt(rs.getTimestamp("deleted_at"));
        return t;
    }
}
