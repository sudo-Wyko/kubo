package com.teamroy.controller;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.teamroy.PasswordUtil;

public class SignupController {
    private static final Logger LOGGER = Logger.getLogger(SignupController.class.getName());
    
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 3;

    private String validateRegistrationInput(String contactNumber, String username, String password) {
        if (contactNumber == null || contactNumber.trim().isEmpty()) {
            return "Contact number cannot be empty.";
        }
        
        // Clean phone number input for syntax evaluation
        String cleanContact = contactNumber.trim().replaceAll("[\\s-]", "");
        
        // PH Standard Mobile Validation Gatekeeper
        if (!cleanContact.matches("^(09|\\+639)\\d{9}$")) {
            return "Please enter a valid PH mobile number (e.g., 09123456789).";
        }
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
        return null; 
    }

    public void handleRegistration(Connection conn, String contactNumber, String username, String password) {
        String validationError = validateRegistrationInput(contactNumber, username, password);
        if (validationError != null) {
            LOGGER.fine(() -> "Registration validation failed: " + validationError);
            throw new IllegalArgumentException(validationError);
        }

        // Standardize the search string formatting
        String targetContact = contactNumber.trim().replaceAll("[\\s-]", "");

        // Pivot query matching parameter target directly to contact_number
        String findTenantSql = "SELECT tenant_id, user_id FROM TENANT WHERE contact_number = ? OR contact_number = ?";
        String insertUserSql = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, 'TENANT')";
        String linkTenantSql = "UPDATE TENANT SET user_id = ? WHERE tenant_id = ?";

        try {
            conn.setAutoCommit(false);
            int targetTenantId = -1;

            // 1. Check if an admin-created tenant profile matches this contact number
            try (PreparedStatement findStmt = conn.prepareStatement(findTenantSql)) {
                // Support looking up either domestic 09... or international +639... styles variations safely
                String alternateContact = targetContact.startsWith("+63") 
                    ? "0" + targetContact.substring(3) 
                    : "+63" + targetContact.substring(1);

                findStmt.setString(1, targetContact);
                findStmt.setString(2, alternateContact);
                
                try (ResultSet rs = findStmt.executeQuery()) {
                    if (rs.next()) {
                        targetTenantId = rs.getInt("tenant_id");
                        Object existingUserId = rs.getObject("user_id");
                        
                        if (existingUserId != null) {
                            throw new IllegalArgumentException("An account has already been claimed and registered with this contact number.");
                        }
                    } else {
                        throw new IllegalArgumentException("No pre-registered profile found with this contact number. Please contact management.");
                    }
                }
            }

            // 2. Insert credentials row
            int generatedUserId = -1;
            try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                userStmt.setString(1, username.trim());
                userStmt.setString(2, PasswordUtil.hash(password)); 
                userStmt.executeUpdate();
                
                try (ResultSet rs = userStmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedUserId = rs.getInt(1);
                    }
                }
            }

            // 3. Link records
            if (generatedUserId != -1 && targetTenantId != -1) {
                try (PreparedStatement linkStmt = conn.prepareStatement(linkTenantSql)) {
                    linkStmt.setInt(1, generatedUserId);
                    linkStmt.setInt(2, targetTenantId);
                    linkStmt.executeUpdate();
                }
            } else {
                throw new SQLException("Failed to retrieve generated account identities.");
            }

            conn.commit();
            LOGGER.info(() -> "Tenant profile claimed via contact number verification: " + username);

        } catch (SQLException e) {
            try {
                conn.rollback(); 
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed during registration transaction.", ex);
            }
            
            if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                throw new IllegalArgumentException("Username already exists. Please choose a different username.");
            } else {
                throw new RuntimeException("Database system error during account claim setup: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                LOGGER.log(Level.SEVERE, "Rollback failed during registration error processing.", ex);
            }
            throw e;
        } finally {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to restore auto-commit behavior.", e);
            }
        }
    }
}