package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class Payment {
    private int paymentId;
    private int tenantId;
    private double amountPaid;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String status; // 'PENDING', 'VERIFIED', 'FAILED'

    public Payment() {
    }

    public Payment(int tenantId, double amountPaid, LocalDateTime paymentDate, String paymentMethod, String status) {
        this.tenantId = tenantId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    // -- Getters --
    public int GetPaymentID() {
        return paymentId;
    }

    public int GetTenantID() {
        return tenantId;
    }

    public double GetAmountPaid() {
        return amountPaid;
    }

    public LocalDateTime GetPaymentDate() {
        return paymentDate;
    }

    public String GetPaymentMethod() {
        return paymentMethod;
    }

    public String GetStatus() {
        return status;
    }

    // -- Setters --
    public void SetPaymentID(int paymentId) {
        this.paymentId = paymentId;
    }

    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public void SetPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public void SetPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public void SetStatus(String status) {
        this.status = status;
    }

}
