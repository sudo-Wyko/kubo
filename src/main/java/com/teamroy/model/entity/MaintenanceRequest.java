package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class MaintenanceRequest {
    private int requestId;
    private int tenantId;
    private Integer roomId; // Integer allows null if not assigned to a specific room
    private String reportDescription;
    private LocalDateTime reportedDate;
    private String status; // 'NEW', 'IN-PROGRESS', 'RESOLVED'

    public MaintenanceRequest() {
    }

    public MaintenanceRequest(int tenantId, Integer roomId, String reportDescription, LocalDateTime reportedDate,
            String status) {
        this.tenantId = tenantId;
        this.roomId = roomId;
        this.reportDescription = reportDescription;
        this.reportedDate = reportedDate;
        this.status = status;
    }

    // -- Getters --
    public int GetRequestID() {
        return requestId;
    }

    public int GetTenantID() {
        return tenantId;
    }

    public Integer GetRoomID() {
        return roomId;
    }

    public String GetReportDescription() {
        return reportDescription;
    }

    public LocalDateTime GetReportedDate() {
        return reportedDate;
    }

    public String GetStatus() {
        return status;
    }

    // -- Setters --
    public void SetRequestID(int requestId) {
        this.requestId = requestId;
    }

    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetRoomID(Integer roomId) {
        this.roomId = roomId;
    }

    public void SetReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }

    public void SetReportedDate(LocalDateTime reportedDate) {
        this.reportedDate = reportedDate;
    }

    public void SetStatus(String status) {
        this.status = status;
    }

}
