package com.teamroy.Model;

public class Lease {
    private int leaseId;
    private String tenantName;
    private String roomNumber;
    private String startDate;
    private String endDate;
    private String status;

    public Lease(int leaseId, String tenantName, String roomNumber, String startDate, String endDate, String status) {
        this.leaseId = leaseId;
        this.tenantName = tenantName;
        this.roomNumber = roomNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public int getLeaseId() { return leaseId; }
    public String getTenantName() { return tenantName; }
    public String getRoomNumber() { return roomNumber; }
    public String getStartDate() { return startDate; }
    public String getEndDate() { return endDate; }
    public String getStatus() { return status; }
}