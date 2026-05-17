package com.teamroy;
public final class Status {
    private Status() {}
    public static final String LEASE_ACTIVE = "ACTIVE";
    public static final String LEASE_EXPIRED = "EXPIRED";
    public static final String LEASE_TERMINATED = "TERMINATED";
    public static final String PAYMENT_PENDING = "PENDING";
    public static final String PAYMENT_VERIFIED = "VERIFIED";
    public static final String PAYMENT_FAILED = "FAILED";
    public static final String MAINT_NEW = "NEW";
    public static final String MAINT_IN_PROGRESS = "IN-PROGRESS";
    public static final String MAINT_RESOLVED = "RESOLVED";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_TENANT = "TENANT";
}
