package com.teamroy.model.dao;

import com.teamroy.model.entity.Tenant;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TenantDaoImpl implements TenantDao {
    private static final Logger LOGGER = Logger.getLogger(TenantDaoImpl.class.getName());
    private final Connection conn;

    public TenantDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Tenant tenant) {
        String sql = "INSERT INTO TENANT (user_id, first_name, last_name, contact_number, email) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (tenant.GetUserID() != null) {
                ps.setInt(1, tenant.GetUserID());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, requireNonBlank(tenant.GetFirstName(), "first_name"));
            ps.setString(3, requireNonBlank(tenant.GetLastName(), "last_name"));
            ps.setString(4, normalizeContact(tenant.GetContactNumber()));
            ps.setString(5, emptyToNull(tenant.GetEmail()));
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DaoException("Tenant insert did not affect any rows.");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    tenant.SetTenantID(rs.getInt(1));
                } else {
                    throw new DaoException("Tenant insert succeeded but no generated key was returned.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert tenant", e);
            throw new DaoException("Could not create tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public Tenant GetByID(int tenantId) {
        String sql = "SELECT * FROM TENANT WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToTenant(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load tenant " + tenantId, e);
        }
        return null;
    }

    @Override
    public List<Tenant> GetAll() {
        return queryTenantList("SELECT * FROM TENANT");
    }

    @Override
    public Tenant GetByUserID(int userId) {
        String sql = "SELECT * FROM TENANT WHERE user_id = ? AND deleted_at IS NULL";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToTenant(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load tenant by user " + userId, e);
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
                while (rs.next()) {
                    tenants.add(resultSetToTenant(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to search tenants by name", e);
            throw new DaoException("Could not search tenants: " + e.getMessage(), e);
        }
        return tenants;
    }

    @Override
    public List<Tenant> GetAllActive() {
        return queryTenantList("SELECT * FROM TENANT WHERE deleted_at IS NULL");
    }

    @Override
    public void Restore(int tenantId) {
        executeUpdate("UPDATE TENANT SET deleted_at = NULL WHERE tenant_id = ?", tenantId);
    }

    @Override
    public List<Tenant> GetTenantsWithBalance() {
        return queryTenantList(
                "SELECT DISTINCT t.* FROM TENANT t "
                        + "JOIN LEASE l ON t.tenant_id = l.tenant_id "
                        + "WHERE l.balance > 0 AND t.deleted_at IS NULL");
    }

    @Override
    public double GetTotalBalance(int tenantId) {
        String sql = "SELECT COALESCE(SUM(balance), 0) FROM LEASE WHERE tenant_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to sum lease balances for tenant " + tenantId, e);
        }
        return 0.0;
    }

    @Override
    public void Update(Tenant tenant) {
        String sql = "UPDATE TENANT SET user_id=?, first_name=?, last_name=?, contact_number=?, email=? WHERE tenant_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (tenant.GetUserID() != null) {
                ps.setInt(1, tenant.GetUserID());
            } else {
                ps.setNull(1, Types.INTEGER);
            }
            ps.setString(2, requireNonBlank(tenant.GetFirstName(), "first_name"));
            ps.setString(3, requireNonBlank(tenant.GetLastName(), "last_name"));
            ps.setString(4, normalizeContact(tenant.GetContactNumber()));
            ps.setString(5, emptyToNull(tenant.GetEmail()));
            ps.setInt(6, tenant.GetTenantID());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update tenant " + tenant.GetTenantID(), e);
            throw new DaoException("Could not update tenant: " + e.getMessage(), e);
        }
    }

    @Override
    public void Delete(int tenantId) {
        executeUpdate("UPDATE TENANT SET deleted_at = CURRENT_TIMESTAMP WHERE tenant_id = ?", tenantId);
    }

    public static String normalizeContact(String contact) {
        if (contact == null || contact.isBlank()) {
            return "N/A";
        }
        String trimmed = contact.trim();
        return trimmed.length() > 20 ? trimmed.substring(0, 20) : trimmed;
    }

    private List<Tenant> queryTenantList(String sql) {
        List<Tenant> tenants = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tenants.add(resultSetToTenant(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Tenant query failed: " + sql, e);
            throw new DaoException("Database query failed: " + e.getMessage(), e);
        }
        return tenants;
    }

    private void executeUpdate(String sql, int tenantId) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Tenant update failed", e);
            throw new DaoException("Database update failed: " + e.getMessage(), e);
        }
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new DaoException(field + " is required.");
        }
        return value.trim();
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Tenant resultSetToTenant(ResultSet rs) throws SQLException {
        Tenant tenant = new Tenant();
        tenant.SetTenantID(rs.getInt("tenant_id"));
        int userId = rs.getInt("user_id");
        tenant.SetUserID(rs.wasNull() ? null : userId);
        tenant.SetFirstName(rs.getString("first_name"));
        tenant.SetLastName(rs.getString("last_name"));
        tenant.SetContactNumber(rs.getString("contact_number"));
        tenant.SetEmail(rs.getString("email"));
        tenant.SetTimeDeletedAt(rs.getTimestamp("deleted_at"));
        return tenant;
    }
}
