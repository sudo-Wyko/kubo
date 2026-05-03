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

    public static void clear() {
        currentUser = null;
        currentTenant = null;
    }
}
