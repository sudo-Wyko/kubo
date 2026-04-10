package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class Document {
    private int documentId;
    private int tenantId;
    private String title;
    private String filePath;
    private LocalDateTime uploadedAt;

    public Document(int tenantId, String title, String filePath, LocalDateTime uploadedAt) {
        this.tenantId = tenantId;
        this.title = title;
        this.filePath = filePath;
        this.uploadedAt = uploadedAt;
    }

    // -- Getters --
    public int GetDocumentID() {
        return documentId;
    }

    public int GetTenantID() {
        return tenantId;
    }

    public String GetTitle() {
        return title;
    }

    public String GetFilePath() {
        return filePath;
    }

    public LocalDateTime GeTimeUploadedAt() {
        return uploadedAt;
    }

    // -- Setters --
    public void SetDocumentID(int documentId) {
        this.documentId = documentId;
    }

    public void SetTenantID(int tenantId) {
        this.tenantId = tenantId;
    }

    public void SetTitle(String title) {
        this.title = title;
    }

    public void SetFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void SetTimeUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
