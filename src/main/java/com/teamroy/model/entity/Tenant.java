package com.teamroy.model.entity;

import java.security.Timestamp;

public class Tenant {
    private int tenantId;
    private Integer userId;
    private String firstName;
    private String lastName;
    private String contactNumber;
    private String email;
    private double totalBalance;
    private Timestamp deletedAt;

    public Tenant(String firstName, String lastName, String contactNumber, String email, double totalBalance) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.totalBalance = totalBalance;
    }

    // -- Getters --
    public int GetTenantID() {
        return tenantId;
    }

    public Integer GetUserID() {
        return userId;
    }

    public String GetFirstName() {
        return firstName;
    }

    public String GetLastName() {
        return lastName;
    }

    public String GetContactNumber() {
        return contactNumber;
    }

    public String GetEmail() {
        return email;
    }

    public double GetTotalBalance() {
        return totalBalance;
    }

    public Timestamp GetTimeDeletedAt() {
        return deletedAt;
    }

    // -- Setters --
    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetUserID(Integer userId) {
        this.userId = userId;
    }

    public void SetFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void SetLastName(String lastName) {
        this.lastName = lastName;
    }

    public void SetContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void SetEmail(String email) {
        this.email = email;
    }

    public void SetTotalBalancd(double totalBalance) {
        this.totalBalance = totalBalance;
    }

    public void SetTimeDeletedAt(Timestamp deletedAt) {
        this.deletedAt = deletedAt;
    }

}
