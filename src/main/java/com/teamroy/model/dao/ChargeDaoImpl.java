package com.teamroy.model.dao;

import com.teamroy.model.entity.Charge;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChargeDaoImpl implements ChargeDao {
    private static final Logger LOGGER = Logger.getLogger(ChargeDaoImpl.class.getName());
    private static final Set<String> ALLOWED_TYPES = Set.of("RENT", "LATE_FEE", "DEPOSIT", "UTILITY");

    private final Connection conn;

    public ChargeDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Charge entity) {
        CreateCharge(entity);
    }

    @Override
    public int CreateCharge(Charge entity) {
        validateCharge(entity);
        String sql = "INSERT INTO CHARGE (lease_id, charge_type, amount, due_date, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.GetLeaseID());
            ps.setString(2, entity.GetChargeType().trim().toUpperCase());
            ps.setDouble(3, entity.GetAmount());
            ps.setDate(4, Date.valueOf(entity.GetDueDate()));
            ps.setString(5, emptyToNull(entity.GetDescription()));
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DaoException("Charge insert did not affect any rows.");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int chargeId = rs.getInt(1);
                    entity.SetChargeID(chargeId);
                    return chargeId;
                }
            }
            throw new DaoException("Charge insert succeeded but no generated key was returned.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert charge for lease " + entity.GetLeaseID(), e);
            throw new DaoException("Could not create charge: " + e.getMessage(), e);
        }
    }

    @Override
    public Charge GetByID(int id) {
        String sql = "SELECT * FROM CHARGE WHERE charge_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToCharge(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load charge " + id, e);
        }
        return null;
    }

    @Override
    public List<Charge> GetAll() {
        List<Charge> charges = new ArrayList<>();
        String sql = "SELECT * FROM CHARGE ORDER BY created_at DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                charges.add(resultSetToCharge(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to list charges", e);
            throw new DaoException("Could not load charges: " + e.getMessage(), e);
        }
        return charges;
    }

    @Override
    public void Update(Charge entity) {
        validateCharge(entity);
        String sql = "UPDATE CHARGE SET lease_id=?, charge_type=?, amount=?, due_date=?, description=? WHERE charge_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.GetLeaseID());
            ps.setString(2, entity.GetChargeType().trim().toUpperCase());
            ps.setDouble(3, entity.GetAmount());
            ps.setDate(4, Date.valueOf(entity.GetDueDate()));
            ps.setString(5, emptyToNull(entity.GetDescription()));
            ps.setInt(6, entity.GetChargeID());
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to update charge " + entity.GetChargeID(), e);
            throw new DaoException("Could not update charge: " + e.getMessage(), e);
        }
    }

    @Override
    public void Delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM CHARGE WHERE charge_id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to delete charge " + id, e);
            throw new DaoException("Could not delete charge: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Charge> GetByLeaseID(int leaseId) {
        List<Charge> charges = new ArrayList<>();
        String sql = "SELECT * FROM CHARGE WHERE lease_id = ? ORDER BY due_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    charges.add(resultSetToCharge(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load charges for lease " + leaseId, e);
            throw new DaoException("Could not load charges: " + e.getMessage(), e);
        }
        return charges;
    }

    @Override
    public List<Charge> GetByTenantID(int tenantId) {
        List<Charge> charges = new ArrayList<>();
        String sql = "SELECT c.* FROM CHARGE c JOIN LEASE l ON c.lease_id = l.lease_id "
                + "WHERE l.tenant_id = ? ORDER BY c.due_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    charges.add(resultSetToCharge(rs));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to load charges for tenant " + tenantId, e);
            throw new DaoException("Could not load charges: " + e.getMessage(), e);
        }
        return charges;
    }

    private static void validateCharge(Charge entity) {
        if (entity == null) {
            throw new DaoException("Charge is required.");
        }
        if (entity.GetLeaseID() <= 0) {
            throw new DaoException("A valid lease is required for a charge.");
        }
        if (entity.GetDueDate() == null) {
            throw new DaoException("Due date is required.");
        }
        if (entity.GetAmount() <= 0) {
            throw new DaoException("Charge amount must be greater than zero.");
        }
        String type = entity.GetChargeType();
        if (type == null || type.isBlank()) {
            throw new DaoException("Charge type is required.");
        }
        String normalized = type.trim().toUpperCase();
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new DaoException("Charge type must be one of: RENT, LATE_FEE, DEPOSIT, UTILITY.");
        }
        entity.SetChargeType(normalized);
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Charge resultSetToCharge(ResultSet rs) throws SQLException {
        Charge charge = new Charge();
        charge.SetChargeID(rs.getInt("charge_id"));
        charge.SetLeaseID(rs.getInt("lease_id"));
        charge.SetChargeType(rs.getString("charge_type"));
        charge.SetAmount(rs.getDouble("amount"));
        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            charge.SetDueDate(dueDate.toLocalDate());
        }
        charge.SetDescription(rs.getString("description"));
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            charge.SetCreatedAt(createdAt.toLocalDateTime());
        }
        return charge;
    }

    public LocalDate GetNextRentDueDate(int tenantId) {
    String sql = "SELECT c.due_date FROM CHARGE c " +
                 "JOIN LEASE l ON c.lease_id = l.lease_id " +
                 "WHERE l.tenant_id = ? AND c.charge_type = 'RENT' " +
                 "AND (c.amount > (SELECT COALESCE(SUM(p.amount), 0) FROM PAYMENT p WHERE p.charge_id = c.charge_id)) " +
                 "ORDER BY c.due_date ASC LIMIT 1";
                 
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, tenantId);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDate("due_date").toLocalDate();
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    
    // Fallback: If they have NO unpaid rent charges, find the LATEST rent charge due date and look 1 month ahead
    return null; 
    }
}
