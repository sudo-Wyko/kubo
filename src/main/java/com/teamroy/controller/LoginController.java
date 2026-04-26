package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
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

        // Use the global utility instead of a local method
        try (Connection conn = DatabaseUtility.getConnection()) {
            if (conn == null) {
                errorLabel.setText("Database connection failed.");
                return;
            }

            // Note: If 'password_hash' in your DB is actually hashed (e.g., BCrypt),
            // a direct string comparison here will fail. You'd need to hash the input
            // first!
            String sql = "SELECT role FROM USER_ACCOUNT WHERE username = ? AND password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                if ("ADMIN".equalsIgnoreCase(role)) {
                    App.setRoot("admin");
                } else {
                    App.setRoot("tenant");
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