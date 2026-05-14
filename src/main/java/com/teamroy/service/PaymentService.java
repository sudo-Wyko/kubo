package com.teamroy.service;
import com.teamroy.model.dao.PaymentDao;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDao;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Payment;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 * Applies tenant balance changes only for {@code VERIFIED} payments. PENDING and FAILED rows never
 * touch {@code TENANT.total_balance} on insert. Status transitions adjust balance at most once
 * (optimistic row update {@code status <=> expected}).
 */
public class PaymentService {
    private static final Logger LOGGER = Logger.getLogger(PaymentService.class.getName());
    private final Connection conn;
    private final PaymentDao paymentDao;
    private final TenantDao tenantDao;
    public PaymentService(Connection conn) {
        this.conn = conn;
        this.paymentDao = new PaymentDaoImpl(conn);
        this.tenantDao = new TenantDaoImpl(conn);
    }
    private static boolean isVerified(String s) {
        return s != null && "VERIFIED".equalsIgnoreCase(s.trim());
    }
    /**
     * Amount to add to {@code TENANT.total_balance} via {@link TenantDao#UpdateBalance(int, double)}.
     * Verified payment reduces what the tenant owes, so delta is negative; reversing verification adds back.
     */
    private static double balanceDeltaForStatusChange(String oldStatus, String newStatus, double amountPaid) {
        boolean was = isVerified(oldStatus);
        boolean now = isVerified(newStatus);
        if (!was && now) {
            return -amountPaid;
        }
        if (was && !now) {
            return amountPaid;
        }
        return 0;
    }
    /**
     * Inserts a payment and, only if status is VERIFIED, decreases tenant balance by the amount.
     */
    public boolean recordNewPayment(Payment payment) {
        if (payment == null) {
            return false;
        }
        boolean priorAc = true;
        try {
            priorAc = conn.getAutoCommit();
            conn.setAutoCommit(false);
            paymentDao.Create(payment);
            if (payment.GetPaymentID() <= 0) {
                conn.rollback();
                LOGGER.warning("Payment insert did not return payment_id");
                return false;
            }
            if (isVerified(payment.GetStatus())) {
                if (!tenantDao.UpdateBalance(payment.GetTenantID(), -payment.GetAmountPaid())) {
                    conn.rollback();
                    LOGGER.severe(() -> "Balance update failed for new VERIFIED payment tenant_id=" + payment.GetTenantID());
                    return false;
                }
                LOGGER.info(() -> String.format(
                        "Tenant balance reduced for new VERIFIED payment: tenant_id=%d payment_id=%d amount=%.2f",
                        payment.GetTenantID(), payment.GetPaymentID(), payment.GetAmountPaid()));
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "recordNewPayment rollback failed", ex);
            }
            LOGGER.log(Level.SEVERE, "recordNewPayment failed", e);
            return false;
        } finally {
            try {
                conn.setAutoCommit(priorAc);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "recordNewPayment: restore autoCommit failed", e);
            }
        }
    }
    /**
     * Updates payment status and applies the correct balance delta. Verifying an already-VERIFIED row
     * is a no-op (no second subtraction). Uses {@code UPDATE ... AND status <=> ?} so concurrent updates
     * do not double-apply.
     */
    public boolean updatePaymentStatus(int paymentId, String newStatus) {
        if (newStatus == null || newStatus.isBlank()) {
            return false;
        }
        String normalizedNew = newStatus.trim();
        Payment row = paymentDao.GetByID(paymentId);
        if (row == null) {
            return false;
        }
        String oldStatus = row.GetStatus();
        if (oldStatus != null && oldStatus.trim().equalsIgnoreCase(normalizedNew)) {
            return true;
        }
        boolean priorAc = true;
        try {
            priorAc = conn.getAutoCommit();
            conn.setAutoCommit(false);
            row = paymentDao.GetByID(paymentId);
            if (row == null) {
                conn.rollback();
                return false;
            }
            oldStatus = row.GetStatus();
            if (oldStatus != null && oldStatus.trim().equalsIgnoreCase(normalizedNew)) {
                conn.commit();
                return true;
            }
            double delta = balanceDeltaForStatusChange(oldStatus, normalizedNew, row.GetAmountPaid());
            if (!paymentDao.updateStatusIfCurrent(paymentId, normalizedNew, oldStatus)) {
                conn.rollback();
                Payment again = paymentDao.GetByID(paymentId);
                if (again != null && again.GetStatus() != null
                        && again.GetStatus().trim().equalsIgnoreCase(normalizedNew)) {
                    LOGGER.fine(() -> "Payment " + paymentId + " already " + normalizedNew + " (idempotent)");
                    return true;
                }
                return false;
            }
            if (delta != 0) {
                final int logTenantId = row.GetTenantID();
                final int logPaymentId = paymentId;
                final double logDelta = delta;
                final String logOldStatus = oldStatus;
                final String logNewStatus = normalizedNew;
                if (!tenantDao.UpdateBalance(logTenantId, logDelta)) {
                    conn.rollback();
                    LOGGER.severe(() -> String.format(
                            "Balance update failed after payment status change: tenant_id=%d payment_id=%d delta=%.2f",
                            logTenantId, logPaymentId, logDelta));
                    return false;
                }
                LOGGER.info(() -> String.format(
                        "Tenant balance adjusted for payment status: tenant_id=%d payment_id=%d %s -> %s delta=%.2f",
                        logTenantId, logPaymentId, String.valueOf(logOldStatus), logNewStatus, logDelta));
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            try {
                if (!conn.getAutoCommit()) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "updatePaymentStatus rollback failed", ex);
            }
            LOGGER.log(Level.SEVERE, "updatePaymentStatus failed payment_id=" + paymentId, e);
            return false;
        } finally {
            try {
                conn.setAutoCommit(priorAc);
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "updatePaymentStatus: restore autoCommit failed", e);
            }
        }
    }
}
