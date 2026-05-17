package com.teamroy.model.dao;

import com.teamroy.model.entity.Payment;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentDao extends GenericDao<Payment> {
    List<Payment> GetByTenantID(int tenantId);

    List<Payment> GetByLeaseID(int leaseId);

    List<Payment> GetByDateRange(LocalDateTime start, LocalDateTime end);

    List<Payment> GetByStatus(String status);

    double GetTotalPaymentsByTenant(int tenantId);

    boolean UpdateStatus(int paymentId, String status);
}
