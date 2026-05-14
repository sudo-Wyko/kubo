package com.teamroy.controller;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.teamroy.PasswordUtil;
public class SignupController {
    private static final Logger LOGGER = Logger.getLogger(SignupController.class.getName());
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;
    private static final int MAX_EMAIL_LENGTH = 50;
    private String validateRegistrationInput(String username, String password, String firstName, String lastName, String email) {
        if (username == null || username.trim().isEmpty()) {
            return "Username cannot be empty.";
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters.";
        }
        if (!username.matches("^[a-zA-Z0-9_.-]+$")) {
            return "Username can only contain letters, numbers, dots, underscores, and hyphens.";
        }
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            return "First name cannot be empty.";
        }
        if (firstName.length() < MIN_NAME_LENGTH || firstName.length() > MAX_NAME_LENGTH) {
            return "First name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.";
        }
        if (!firstName.matches("^[a-zA-Z\\s'-]+$")) {
            return "First name can only contain letters, spaces, hyphens, and apostrophes.";
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            return "Last name cannot be empty.";
        }
        if (lastName.length() < MIN_NAME_LENGTH || lastName.length() > MAX_NAME_LENGTH) {
            return "Last name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.";
        }
        if (!lastName.matches("^[a-zA-Z\\s'-]+$")) {
            return "Last name can only contain letters, spaces, hyphens, and apostrophes.";
        }
        if (email != null && !email.isBlank()) {
            String trimmed = email.trim();
            if (trimmed.length() > MAX_EMAIL_LENGTH) {
                return "Email must be at most " + MAX_EMAIL_LENGTH + " characters.";
            }
            if (!trimmed.matches("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return "Please enter a valid email address.";
            }
        }
        return null;
    }
    /**
     * Finds an active tenant row added by admin (no user yet) whose email matches signup, case-insensitive.
     */
    private Integer findUnlinkedTenantIdByEmail(Connection conn, String normalizedEmail) throws SQLException {
        String sql = "SELECT tenant_id FROM TENANT WHERE user_id IS NULL AND deleted_at IS NULL "
                + "AND email IS NOT NULL AND TRIM(email) <> '' AND LOWER(TRIM(email)) = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, normalizedEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }
    public void handleRegistration(Connection conn, String username, String password, String firstName, String lastName, String email) {
        String validationError = validateRegistrationInput(username, password, firstName, lastName, email);
        if (validationError != null) {
            LOGGER.fine(() -> "Registration validation failed: " + validationError);
            throw new IllegalArgumentException(validationError);
        }
        try {
            conn.setAutoCommit(false);
        String insertUser = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, 'TENANT')";
        try (PreparedStatement userStmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
            userStmt.setString(1, username);
            userStmt.setString(2, PasswordUtil.hash(password)); 
            userStmt.executeUpdate();
            int generatedUserId = -1;
            try (ResultSet rs = userStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedUserId = rs.getInt(1);
                }
            }
            if (generatedUserId != -1) {
                String signupEmail = email == null ? "" : email.trim();
                if (signupEmail.isEmpty()) {
                    String insertTenant = "INSERT INTO TENANT (user_id, first_name, last_name) VALUES (?, ?, ?)";
                    try (PreparedStatement tenantStmt = conn.prepareStatement(insertTenant)) {
                        tenantStmt.setInt(1, generatedUserId);
                        tenantStmt.setString(2, firstName);
                        tenantStmt.setString(3, lastName);
                        tenantStmt.executeUpdate();
                    }
                } else {
                    String normalized = signupEmail.toLowerCase(Locale.ROOT);
                    Integer orphanTenantId = findUnlinkedTenantIdByEmail(conn, normalized);
                    if (orphanTenantId != null) {
                        String linkSql = "UPDATE TENANT SET user_id = ? WHERE tenant_id = ? AND user_id IS NULL";
                        try (PreparedStatement linkStmt = conn.prepareStatement(linkSql)) {
                            linkStmt.setInt(1, generatedUserId);
                            linkStmt.setInt(2, orphanTenantId);
                            int linked = linkStmt.executeUpdate();
                            if (linked > 0) {
                                LOGGER.info(() -> "Linked new user to existing tenant_id=" + orphanTenantId + " by email.");
                            }
                        }
                    } else {
                        String insertTenant = "INSERT INTO TENANT (user_id, first_name, last_name, email) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement tenantStmt = conn.prepareStatement(insertTenant)) {
                            tenantStmt.setInt(1, generatedUserId);
                            tenantStmt.setString(2, firstName);
                            tenantStmt.setString(3, lastName);
                            tenantStmt.setString(4, signupEmail);
                            tenantStmt.executeUpdate();
                        }
                    }
                }
            }
        }
        conn.commit();
        LOGGER.info(() -> "Tenant account created successfully: " + username);
    } catch (SQLException e) {
        try {
            conn.rollback(); 
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new IllegalArgumentException("Username already exists. Please choose a different username.");
            } else {
                throw new RuntimeException("Database error during registration: " + e.getMessage(), e);
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Rollback failed during registration.", ex);
        }
    } catch (Exception e) {
        try {
            conn.rollback();
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Rollback failed during registration.", ex);
        }
        throw new RuntimeException("Registration error: " + e.getMessage(), e);
    } finally {
        try {
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to restore auto-commit.", e);
        }
    }
    }
}
