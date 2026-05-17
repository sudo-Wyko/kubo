package com.teamroy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class ExportFileNames {
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyy-MM");

    private ExportFileNames() {
    }

    public static String monthPrefix() {
        return LocalDate.now().format(MONTH);
    }

    public static String tenantsCsv() {
        return monthPrefix() + "-TENANTS.csv";
    }

    public static String paymentsCsv() {
        return monthPrefix() + "-PAYMENTS.csv";
    }

    public static String paymentsJson() {
        return monthPrefix() + "-PAYMENTS.json";
    }
}
