package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class ActivityLog {
    private int activityId;
    private int tenantId;
    private String description;
    private LocalDateTime createdAt;

    public ActivityLog() {
    }

    public ActivityLog(int tenantId, String description, LocalDateTime createdAt) {
        this.tenantId = tenantId;
        this.description = description;
        this.createdAt = createdAt;
    }

    // -- Getters --
    public int GetActivityID() {
        return activityId;
    }

    public int GetTenantID() {
        return tenantId;
    }

    public String GetDescription() {
        return description;
    }

    public LocalDateTime GetCreatedAt() {
        return createdAt;
    }

    // -- Setters --
    public void SetActivityID(int activityId) {
        this.activityId = activityId;
    }

    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetDescription(String description) {
        this.description = description;
    }

    public void SetCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}