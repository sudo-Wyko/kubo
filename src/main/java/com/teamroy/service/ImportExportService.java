package com.teamroy.service;

import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.TenantDao;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ImportExportService {

    public void exportTenantsToCSV(List<Tenant> tenants, String filePath) throws IOException {
        TenantDao balanceDao = new TenantDaoImpl(ConnectionManager.getConnection());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            bw.write("tenant_id,first_name,last_name,email,contact,balance");
            bw.newLine();
            for (Tenant t : tenants) {
                double balance = balanceDao.GetTotalBalance(t.GetTenantID());
                bw.write(t.GetTenantID() + "," + escape(t.GetFirstName()) + ","
                        + escape(t.GetLastName()) + "," + escape(t.GetEmail()) + ","
                        + escape(t.GetContactNumber()) + "," + balance);
                bw.newLine();
            }
        }
    }

    public void exportPaymentsToCSV(List<Payment> payments, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            bw.write("payment_id,lease_id,amount_paid,payment_date,payment_method,status");
            bw.newLine();
            for (Payment p : payments) {
                String when = p.GetPaymentDate() == null ? "" : p.GetPaymentDate().toString().replace(",", " ");
                bw.write(p.GetPaymentID() + "," + p.GetLeaseID() + "," + p.GetAmountPaid() + ","
                        + escape(when) + "," + escape(p.GetPaymentMethod()) + "," + escape(p.GetStatus()));
                bw.newLine();
            }
        }
    }

    public void exportPaymentsToJSON(List<Payment> payments, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath, StandardCharsets.UTF_8))) {
            bw.write("[");
            for (int i = 0; i < payments.size(); i++) {
                Payment p = payments.get(i);
                if (i > 0) {
                    bw.write(",");
                }
                bw.newLine();
                bw.write("{");
                bw.write("\"paymentId\":" + p.GetPaymentID() + ",");
                bw.write("\"leaseId\":" + p.GetLeaseID() + ",");
                bw.write("\"amountPaid\":" + p.GetAmountPaid() + ",");
                bw.write("\"paymentDate\":"
                        + (p.GetPaymentDate() == null ? "null"
                        : "\"" + jsonEscape(p.GetPaymentDate().toString()) + "\"") + ",");
                bw.write("\"paymentMethod\":\"" + jsonEscape(p.GetPaymentMethod()) + "\",");
                bw.write("\"status\":\"" + jsonEscape(p.GetStatus()) + "\"");
                bw.write("}");
            }
            bw.newLine();
            bw.write("]");
        }
    }

    public int[] importTenantsFromCSV(String filePath, TenantDao dao) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(filePath), StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return new int[]{0, 0};
        }
        int imported = 0;
        int skipped = 0;
        Integer idxFirstName = null;
        Integer idxLastName = null;
        Integer idxEmail = null;
        Integer idxContact = null;
        
        List<String> firstCols = splitCsv(lines.get(0));
        boolean hasHeaderRow = false;
        for (String column : firstCols) {
            if ("first_name".equalsIgnoreCase(column.trim())) {
                hasHeaderRow = true;
                break;
            }
        }
        
        int startIdx;
        if (hasHeaderRow) {
            idxFirstName = columnIndex(firstCols, "first_name");
            idxLastName = columnIndex(firstCols, "last_name");
            idxEmail = columnIndex(firstCols, "email");
            idxContact = columnIndex(firstCols, "contact");
            startIdx = 1;
            if (idxFirstName == null || idxLastName == null) {
                System.err.println("CSV header missing first_name / last_name column names.");
                return new int[]{0, lines.size()};
            }
        } else {
            // FIX: Array entries are 0-indexed when parsing a raw file without headers
            idxFirstName = 0; 
            idxLastName = 1;
            idxEmail = 2;
            idxContact = 3;
            startIdx = 0;
        }
        
        for (int i = startIdx; i < lines.size(); i++) {
            String raw = lines.get(i);
            if (raw == null || raw.isBlank()) {
                skipped++;
                continue;
            }
            List<String> cols = splitCsv(raw);
            
            // Check that the line has enough parsed columns to fulfill required names
            if (cols.size() <= Math.max(idxFirstName, idxLastName)) {
                skipped++;
                continue;
            }
            
            String firstName = unquote(cols.get(idxFirstName).trim());
            String lastName = unquote(cols.get(idxLastName).trim());
            String email = idxEmail != null && idxEmail < cols.size() ? unquote(cols.get(idxEmail).trim()) : "";
            String contact = idxContact != null && idxContact < cols.size() ? unquote(cols.get(idxContact).trim()) : "";
            
            if (firstName.isBlank() || lastName.isBlank()) {
                skipped++;
                continue;
            }
            
            // Map strings into a structured object entity
            Tenant tenant = new Tenant(firstName, lastName, TenantDaoImpl.normalizeContact(contact), email);
            tenant.SetUserID(null); // Explicitly unlinked from a user account profile on initial import
            
            // Persist the transaction down to the storage engine
            dao.Create(tenant);
            imported++;
        }
        return new int[]{imported, skipped};
    }

    private static Integer columnIndex(List<String> header, String key) {
        for (int i = 0; i < header.size(); i++) {
            if (key.equalsIgnoreCase(header.get(i).trim())) {
                return i;
            }
        }
        return null;
    }

    private static List<String> splitCsv(String line) {
        List<String> cols = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cols.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        cols.add(sb.toString());
        return cols;
    }

    private static String unquote(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) {
            return t.substring(1, t.length() - 1).replace("\"\"", "\"");
        }
        return t;
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String jsonEscape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "").replace("\n", "\\n");
    }
}
