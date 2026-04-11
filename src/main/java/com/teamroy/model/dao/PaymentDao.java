package com.teamroy.model.dao;

import com.teamroy.model.entity.Payment;
import java.util.List;
import java.time.LocalDateTime;

public interface PaymentDao extends GenericDao<Payment> {

    List<Payment> GetByTenantId(int tenantId);

    List<Payment> GetByDateRange(LocalDateTime start, LocalDateTime end);

    List<Payment> GetByStatus(String status);

    double GetTotalPaymentsByTenant(int tenantId);

    boolean UpdateStatus(int paymentId, String status);
}
