package com.teamroy;

public class SessionManager {
    private static int currentUserId = -1;
    private static String currentUserRole = null;
    private static int currentTenantId = -1; // Specific to tenants

    // Call this when a user successfully logs in
    public static void loginUser(int userId, String role) {
        currentUserId = userId;
        currentUserRole = role;
    }

    // Call this when you fetch the tenant profile
    public static void setTenantId(int tenantId) {
        currentTenantId = tenantId;
    }

    // Call this when the user logs out
    public static void logout() {
        currentUserId = -1;
        currentUserRole = null;
        currentTenantId = -1;
    }

    // Getters for your controllers to use
    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static int getCurrentTenantId() {
        return currentTenantId;
    }
}