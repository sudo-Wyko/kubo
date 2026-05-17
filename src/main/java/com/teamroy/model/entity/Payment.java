package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class Payment {
    private int paymentId;
    private int leaseId;
    private double amountPaid;
    private LocalDateTime paymentDate;
    private String paymentMethod;
    private String status;

    public Payment() {
    }

    public Payment(int leaseId, double amountPaid, LocalDateTime paymentDate, String paymentMethod, String status) {
        this.leaseId = leaseId;
        this.amountPaid = amountPaid;
        this.paymentDate = paymentDate;
        this.paymentMethod = paymentMethod;
        this.status = status;
    }

    public int GetPaymentID() {
        return paymentId;
    }

    public int GetLeaseID() {
        return leaseId;
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

    public void SetPaymentID(int paymentId) {
        this.paymentId = paymentId;
    }

    public void SetLeaseID(int leaseId) {
        this.leaseId = leaseId;
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
