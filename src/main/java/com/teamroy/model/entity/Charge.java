package com.teamroy.model.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Charge {
    private int chargeId;
    private int leaseId;
    private String chargeType;
    private double amount;
    private LocalDate dueDate;
    private String description;
    private LocalDateTime createdAt;

    public Charge() {
    }

    public Charge(int leaseId, String chargeType, double amount, LocalDate dueDate, String description) {
        this.leaseId = leaseId;
        this.chargeType = chargeType;
        this.amount = amount;
        this.dueDate = dueDate;
        this.description = description;
    }

    public int GetChargeID() {
        return chargeId;
    }

    public int GetLeaseID() {
        return leaseId;
    }

    public String GetChargeType() {
        return chargeType;
    }

    public double GetAmount() {
        return amount;
    }

    public LocalDate GetDueDate() {
        return dueDate;
    }

    public String GetDescription() {
        return description;
    }

    public LocalDateTime GetCreatedAt() {
        return createdAt;
    }

    public void SetChargeID(int chargeId) {
        this.chargeId = chargeId;
    }

    public void SetLeaseID(int leaseId) {
        this.leaseId = leaseId;
    }

    public void SetChargeType(String chargeType) {
        this.chargeType = chargeType;
    }

    public void SetAmount(double amount) {
        this.amount = amount;
    }

    public void SetDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public void SetDescription(String description) {
        this.description = description;
    }

    public void SetCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
