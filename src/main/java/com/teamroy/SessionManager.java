package com.teamroy;
import com.teamroy.model.entity.Tenant;
import com.teamroy.model.entity.UserAccount;
public final class SessionManager {
    private static UserAccount currentUser;
    private static Tenant currentTenant;
    private SessionManager() {
    }
    public static void setCurrentUser(UserAccount user) {
        currentUser = user;
    }
    public static UserAccount getCurrentUser() {
        return currentUser;
    }
    public static void setCurrentTenant(Tenant tenant) {
        currentTenant = tenant;
    }
    public static Tenant getCurrentTenant() {
        return currentTenant;
    }
    public static int getCurrentUserId() {
        return currentUser != null ? currentUser.GetUserID() : -1;
    }
    public static int getCurrentTenantId() {
        return currentTenant != null ? currentTenant.GetTenantID() : -1;
    }
    public static void logout() {
        clear();
    }
    public static void clear() {
        currentUser = null;
        currentTenant = null;
    }
}
