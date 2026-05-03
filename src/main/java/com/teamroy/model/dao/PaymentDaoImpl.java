package com.teamroy.model.dao;

import com.teamroy.model.entity.Payment;
import java.time.LocalDateTime;
import java.util.*;
import java.sql.*;

public class PaymentDaoImpl implements PaymentDao {
    private Connection conn;

    public PaymentDaoImpl(Connection conn) {
        this.conn = conn;
    }

    @Override
    public void Create(Payment entity) {
        String sql = "INSERT INTO PAYMENT (tenant_id, amount_paid, payment_date, payment_method, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, entity.GetTenantID());
            ps.setDouble(2, entity.GetAmountPaid());
            // Passing LocalDateTime directly to JDBC
            ps.setObject(3, entity.GetPaymentDate());
            ps.setString(4, entity.GetPaymentMethod());
            ps.setString(5, entity.GetStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next())
                    entity.SetPaymentID(rs.getInt(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Payment GetByID(int id) {
        String sql = "SELECT * FROM PAYMENT WHERE payment_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return ResultSetToPayment(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<Payment> GetAll() {
        List<Payment> payments = new ArrayList<>();
        String sql = "SELECT * FROM PAYMENT";
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next())
                payments.add(ResultSetToPayment(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public void Update(Payment entity) {
        String sql = "UPDATE PAYMENT SET tenant_id=?, amount_paid=?, payment_date=?, payment_method=?, status=? WHERE payment_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, entity.GetTenantID());
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
        String sql = "SELECT * FROM PAYMENT WHERE tenant_id = ? ORDER BY payment_date DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    payments.add(ResultSetToPayment(rs));
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
                while (rs.next())
                    payments.add(ResultSetToPayment(rs));
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
                while (rs.next())
                    payments.add(ResultSetToPayment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return payments;
    }

    @Override
    public double GetTotalPaymentsByTenant(int tenantId) {
        String sql = "SELECT SUM(amount_paid) FROM PAYMENT WHERE tenant_id = ? AND status = 'VERIFIED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getDouble(1);
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

    private Payment ResultSetToPayment(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.SetPaymentID(rs.getInt("payment_id"));
        p.SetTenantID(rs.getInt("tenant_id"));
        p.SetAmountPaid(rs.getDouble("amount_paid"));
        // Using rs.getObject with LocalDateTime.class
        p.SetPaymentDate(rs.getObject("payment_date", LocalDateTime.class));
        p.SetPaymentMethod(rs.getString("payment_method"));
        p.SetStatus(rs.getString("status"));
        return p;
    }

}
