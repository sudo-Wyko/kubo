package com.teamroy.Model;

public class Tenant {
    private int tenantId;
    private String fullName;
    private String contactNumber;
    private String email;
    private String status;
    private String roomNumber;
    private double balance;
    private String leaseEndDate;

    public Tenant(int tenantId, String fullName, String contactNumber, String email, String status, String roomNumber, double balance,String leaseEndDate) {
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.contactNumber = contactNumber;
        this.email = email;
        this.status = status;
        this.roomNumber = roomNumber;
        this.balance = balance;
        this.leaseEndDate = leaseEndDate;
    }

    public int getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public String getContactNumber() { return contactNumber; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public String getRoomNumber() { return roomNumber; }
    public double getBalance() { return balance; }
    public String getLeaseEndDate() { return leaseEndDate;}
}