package com.teamroy.model.entity;

import java.time.LocalDateTime;

public class Announcement {
    private int announcementId;
    private String title;
    private String message;
    private LocalDateTime datePosted;

    public Announcement() {
    }

    public Announcement(String title, String message, LocalDateTime datePosted) {
        this.title = title;
        this.message = message;
        this.datePosted = datePosted;
    }

    // -- Getters --
    public int GetAnnouncementID() {
        return announcementId;
    }

    public String GetTitle() {
        return title;
    }

    public String GetMessage() {
        return message;
    }

    public LocalDateTime GetDatePosted() {
        return datePosted;
    }

    // -- Setters --
    public void SetAnnouncementID(int announcementId) {
        this.announcementId = announcementId;
    }

    public void SetTitle(String title) {
        this.title = title;
    }

    public void SetMessage(String message) {
        this.message = message;
    }

    public void SetDatePosted(LocalDateTime datePosted) {
        this.datePosted = datePosted;
    }
}