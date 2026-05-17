package com.teamroy.service;

import com.teamroy.model.dao.PaymentDao;
import com.teamroy.model.dao.PaymentDaoImpl;

import java.sql.Connection;

public class PaymentService {
    private final PaymentDao paymentDao;

    public PaymentService(Connection conn) {
        this.paymentDao = new PaymentDaoImpl(conn);
    }

    public boolean verifyPayment(int paymentId) {
        return paymentDao.UpdateStatus(paymentId, "VERIFIED");
    }
}
