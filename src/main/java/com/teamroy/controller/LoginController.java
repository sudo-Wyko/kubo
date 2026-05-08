package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
import com.teamroy.SessionManager; // Make sure this import matches where your SessionManager is located!
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter credentials.");
            return;
        }

        try (Connection conn = DatabaseUtility.getConnection()) {
            if (conn == null) {
                errorLabel.setText("Database connection failed.");
                return;
            }

            // 1. Grab user_id as well as the role
            String sql = "SELECT user_id, role FROM USER_ACCOUNT WHERE username = ? AND password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");

                // 2. Save the User ID and Role to the SessionManager
                SessionManager.loginUser(userId, role);

                if ("ADMIN".equalsIgnoreCase(role)) {
                    App.setRoot("admin");
                } else {
                    // 3. If it's a tenant, fetch their tenant_id using their user_id
                    String tenantSql = "SELECT tenant_id FROM TENANT WHERE user_id = ?";
                    PreparedStatement tenantStmt = conn.prepareStatement(tenantSql);
                    tenantStmt.setInt(1, userId);
                    ResultSet tenantRs = tenantStmt.executeQuery();

                    if (tenantRs.next()) {
                        int tenantId = tenantRs.getInt("tenant_id");

                        // 4. Save the Tenant ID to the SessionManager
                        SessionManager.setTenantId(tenantId);

                        // 5. Now it's safe to load the tenant dashboard
                        App.setRoot("tenant");
                    } else {
                        errorLabel.setText("Login failed: No tenant profile linked to this account.");
                    }
                }
            } else {
                errorLabel.setText("Invalid username or password.");
            }
        } catch (Exception e) {
            errorLabel.setText("Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}