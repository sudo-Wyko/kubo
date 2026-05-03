package com.teamroy.model.entity;

public class UserAccount {
    private int userId;
    private String username;
    private String passwordHash;
    private String role;

    public UserAccount() {
    }

    public UserAccount(String username, String passwordHash, String role) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    // -- Getters --
    public int GetUserID() {
        return userId;
    }

    public String GetUsername() {
        return username;
    }

    public String GetPassword() {
        return passwordHash;
    }

    public String GetRole() {
        return role;
    }

    // -- Setters --
    public void SetUserID(int userId) {
        this.userId = userId;
    }

    public void SetUsername(String username) {
        this.username = username;
    }

    public void SetPassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void SetRole(String role) {
        this.role = role;
    }

}
