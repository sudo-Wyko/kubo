package com.teamroy.model.dao;

import com.teamroy.model.entity.Payment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PaymentDaoImpl implements PaymentDao {
    private static final Logger LOGGER = Logger.getLogger(PaymentDaoImpl.class.getName());
    private static final Set<String> ALLOWED_STATUS = Set.of("PENDING", "VERIFIED", "FAILED");

    private final Connection conn;

    public PaymentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Payment entity) {
        validatePayment(entity);
        String sql = "INSERT INTO PAYMENT (lease_id, amount_paid, payment_date, payment_method, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.GetLeaseID());
            ps.setDouble(2, entity.GetAmountPaid());
            ps.setObject(3, entity.GetPaymentDate() == null ? LocalDateTime.now() : entity.GetPaymentDate());
            ps.setString(4, entity.GetPaymentMethod());
            ps.setString(5, entity.GetStatus().trim().toUpperCase());
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new DaoException("Payment insert did not affect any rows.");
            }
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    entity.SetPaymentID(rs.getInt(1));
                } else {
                    throw new DaoException("Payment insert succeeded but no generated key was returned.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to insert payment for lease " + entity.GetLeaseID(), e);
            throw new DaoException("Could not create payment: " + e.getMessage(), e);
        }
    }

    private static void validatePayment(Payment entity) {
        if (entity == null) {
            throw new DaoException("Payment is required.");
        }
        if (entity.GetLeaseID() <= 0) {
            throw new DaoException("A valid lease is required for a payment.");
        }
        if (entity.GetAmountPaid() <= 0) {
            throw new DaoException("Payment amount must be greater than zero.");
        }
        String status = entity.GetStatus();
        if (status == null || status.isBlank()) {
            throw new DaoException("Payment status is required.");
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUS.contains(normalized)) {
            throw new DaoException("Payment status must be PENDING, VERIFIED, or FAILED.");
        }
        entity.SetStatus(normalized);
    }

    @Override
    public Payment GetByID(int id) {
        String sql = "SELECT * FROM PAYMENT WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return resultSetToPayment(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Payment> GetAll() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT ORDER BY payment_date DESC";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                payments.add(resultSetToPayment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public void Update(Payment entity) {
        String sql = "UPDATE PAYMENT SET lease_id=?, amount_paid=?, payment_date=?, payment_method=?, status=? WHERE payment_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.GetLeaseID());
            ps.setDouble(2, entity.GetAmountPaid());
            ps.setObject(3, entity.GetPaymentDate());
            ps.setString(4, entity.GetPaymentMethod());
            ps.setString(5, entity.GetStatus());
            ps.setInt(6, entity.GetPaymentID());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void Delete(int id) {
        String sql = "DELETE FROM PAYMENT WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Payment> GetByTenantID(int tenantId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT p.* FROM PAYMENT p JOIN LEASE l ON p.lease_id = l.lease_id WHERE l.tenant_id = ? ORDER BY p.payment_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(resultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public List<Payment> GetByLeaseID(int leaseId) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT WHERE lease_id = ? ORDER BY payment_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, leaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(resultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public List<Payment> GetByDateRange(LocalDateTime start, LocalDateTime end) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT WHERE payment_date BETWEEN ? AND ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, start);
            ps.setObject(2, end);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(resultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public List<Payment> GetByStatus(String status) {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT WHERE status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(resultSetToPayment(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public double GetTotalPaymentsByTenant(int tenantId) {
        String sql = "SELECT COALESCE(SUM(p.amount_paid), 0) FROM PAYMENT p "
                + "JOIN LEASE l ON p.lease_id = l.lease_id WHERE l.tenant_id = ? AND p.status = 'VERIFIED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    @Override
    public boolean UpdateStatus(int paymentId, String status) {
        String sql = "UPDATE PAYMENT SET status = ? WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, paymentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Payment resultSetToPayment(ResultSet rs) throws SQLException {
        Payment payment = new Payment();
        payment.SetPaymentID(rs.getInt("payment_id"));
        payment.SetLeaseID(rs.getInt("lease_id"));
        payment.SetAmountPaid(rs.getDouble("amount_paid"));
        payment.SetPaymentDate(rs.getObject("payment_date", LocalDateTime.class));
        payment.SetPaymentMethod(rs.getString("payment_method"));
        payment.SetStatus(rs.getString("status"));
        return payment;
    }
}
