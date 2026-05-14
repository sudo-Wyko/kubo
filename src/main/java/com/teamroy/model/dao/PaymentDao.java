package com.teamroy.model.dao;
import com.teamroy.model.entity.Payment;
import java.util.List;
import java.time.LocalDateTime;
public interface PaymentDao extends GenericDao<Payment> {
    List<Payment> GetByTenantID(int tenantId);
    List<Payment> GetByDateRange(LocalDateTime start, LocalDateTime end);
    List<Payment> GetByStatus(String status);
    double GetTotalPaymentsByTenant(int tenantId);
    boolean UpdateStatus(int paymentId, String status);
    /**
     * Sets status only if the row still matches {@code expectedCurrentStatus} (including NULL).
     * Used with {@link com.teamroy.service.PaymentService} for idempotent verify and consistent transitions.
     */
    boolean updateStatusIfCurrent(int paymentId, String newStatus, String expectedCurrentStatus);
}
