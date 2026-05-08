package com.teamroy.service;
import com.teamroy.model.dao.PaymentDao;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDao;
import com.teamroy.model.dao.TenantDaoImpl;
import java.sql.Connection;
public class PaymentService {
    private final PaymentDao paymentDao;
    private final TenantDao tenantDao;
    public PaymentService(Connection conn) {
        this.paymentDao = new PaymentDaoImpl(conn);
        this.tenantDao = new TenantDaoImpl(conn);
    }
    public boolean verifyPayment(int paymentId, int tenantId, double amount) {
        boolean updated = paymentDao.UpdateStatus(paymentId, "VERIFIED");
        if (updated) {
            tenantDao.UpdateBalance(tenantId, -amount);
        }
        return updated;
    }
}
