package com.teamroy.model.entity;

import java.time.LocalDate;

public class Lease {
    private int leaseId;
    private int tenantId;
    private int roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    private double monthlyRent;
    private String status; // 'ACTIVE', 'EXPIRED', 'TERMINATED'

    public Lease() {
    }

    public Lease(int tenantId, int roomId, LocalDate startDate, LocalDate endDate, double monthlyRent, String status) {
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.monthlyRent = monthlyRent;
        this.status = status;
    }

    // -- Getters --
    public int GetLeaseID() {
        return leaseId;
    }

    public int GetTenantID() {
        return tenantId;
    }

    public int GetRoomID() {
        return roomId;
    }

    public LocalDate GetStartDate() {
        return startDate;
    }

    public LocalDate GetEndDate() {
        return endDate;
    }

    public double GetMonthlyRent() {
        return monthlyRent;
    }

    public String GetStatus() {
        return status;
    }

    // -- Setters --
    public void SetLeaseID(int leaseId) {
        this.leaseId = leaseId;
    }

    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetRoomID(int roomId) {
        this.roomId = roomId;
    }

    public void SetStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void SetEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public void SetMonthlyRent(double monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public void SetStatus(String status) {
        this.status = status;
    }
}
