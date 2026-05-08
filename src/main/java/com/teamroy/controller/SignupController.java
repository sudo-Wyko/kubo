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
    
    // Validation constants
    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MAX_USERNAME_LENGTH = 50;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MIN_NAME_LENGTH = 2;
    private static final int MAX_NAME_LENGTH = 50;
    
    /**
     * Validates tenant registration input
     * @return Error message if validation fails, null if validation passes
     */
    private String validateRegistrationInput(String username, String password, String firstName, String lastName) {
        // Username validation
        if (username == null || username.trim().isEmpty()) {
            return "Username cannot be empty.";
        }
        if (username.length() < MIN_USERNAME_LENGTH || username.length() > MAX_USERNAME_LENGTH) {
            return "Username must be between " + MIN_USERNAME_LENGTH + " and " + MAX_USERNAME_LENGTH + " characters.";
        }
        if (!username.matches("^[a-zA-Z0-9_.-]+$")) {
            return "Username can only contain letters, numbers, dots, underscores, and hyphens.";
        }
        
        // Password validation
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty.";
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        }
        
        // First name validation
        if (firstName == null || firstName.trim().isEmpty()) {
            return "First name cannot be empty.";
        }
        if (firstName.length() < MIN_NAME_LENGTH || firstName.length() > MAX_NAME_LENGTH) {
            return "First name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.";
        }
        if (!firstName.matches("^[a-zA-Z\\s'-]+$")) {
            return "First name can only contain letters, spaces, hyphens, and apostrophes.";
        }
        
        // Last name validation
        if (lastName == null || lastName.trim().isEmpty()) {
            return "Last name cannot be empty.";
        }
        if (lastName.length() < MIN_NAME_LENGTH || lastName.length() > MAX_NAME_LENGTH) {
            return "Last name must be between " + MIN_NAME_LENGTH + " and " + MAX_NAME_LENGTH + " characters.";
        }
        if (!lastName.matches("^[a-zA-Z\\s'-]+$")) {
            return "Last name can only contain letters, spaces, hyphens, and apostrophes.";
        }
        
        return null; // All validations passed
    }
    
    public void handleRegistration(Connection conn, String username, String password, String firstName, String lastName) {
        // Validate input first
        String validationError = validateRegistrationInput(username, password, firstName, lastName);
        if (validationError != null) {
            LOGGER.fine(() -> "Registration validation failed: " + validationError);
            throw new IllegalArgumentException(validationError);
        }
    
        // We use a transaction because we are touching two tables!
        try {
            conn.setAutoCommit(false);

        // 1. Insert the credentials and ask MySQL to give us the new ID back
        String insertUser = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, 'TENANT')";
        
        // Notice the special flag here: Statement.RETURN_GENERATED_KEYS
        try (PreparedStatement userStmt = conn.prepareStatement(insertUser, Statement.RETURN_GENERATED_KEYS)) {
            userStmt.setString(1, username);
            userStmt.setString(2, PasswordUtil.hash(password)); // Hash the password!
            userStmt.executeUpdate();

            // 2. Retrieve the auto-incremented user_id MySQL just created
            int generatedUserId = -1;
            try (ResultSet rs = userStmt.getGeneratedKeys()) {
                if (rs.next()) {
                    generatedUserId = rs.getInt(1);
                }
            }

            // 3. Now, link that user_id to their actual Tenant profile
            if (generatedUserId != -1) {
                String insertTenant = "INSERT INTO TENANT (user_id, first_name, last_name) VALUES (?, ?, ?)";
                try (PreparedStatement tenantStmt = conn.prepareStatement(insertTenant)) {
                    tenantStmt.setInt(1, generatedUserId);
                    tenantStmt.setString(2, firstName);
                    tenantStmt.setString(3, lastName);
                    tenantStmt.executeUpdate();
                }
            }
        }

        // 4. If both inserts worked perfectly, commit it to the database
        conn.commit();
        LOGGER.info(() -> "Tenant account created successfully: " + username);

    } catch (SQLException e) {
        try {
            conn.rollback(); // Undo everything if something broke
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
