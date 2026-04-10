package com.teamroy.Controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.teamroy.App;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    // MySQL Connection Settings
    // Make sure the database name matches what you created in Step 2!
    private static final String DB_URL = "jdbc:mysql://localhost:3306/kubo_db";
    private static final String DB_USER = "root"; // Your MySQL username (usually 'root')
    private static final String DB_PASSWORD = "Jfrancis@1225"; // Your MySQL password

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        // Connect to MySQL
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            String sql = "SELECT * FROM USER_ACCOUNT WHERE username = ? AND password_hash = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // 1. Grab the role from the database row that matched the login
                String role = rs.getString("role");

                errorLabel.setStyle("-fx-text-fill: #22c55e;"); // Green
                errorLabel.setText("Login Successful! Loading...");

                // 2. Route the user based on their role!
                if ("ADMIN".equalsIgnoreCase(role)) {
                    App.setRoot("admin");
                } else if ("TENANT".equalsIgnoreCase(role)) {
                    App.setRoot("tenant");
                } else {
                    errorLabel.setStyle("-fx-text-fill: #ef4444;"); // Red
                    errorLabel.setText("Error: Unknown user role.");
                }

            } else {
                errorLabel.setStyle("-fx-text-fill: #ef4444;"); // Red
                errorLabel.setText("Invalid username or password.");
            }

        } catch (Exception e) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setText("Failed to connect to MySQL server.");
            e.printStackTrace(); // This prints the exact error in your VS Code terminal
        }
    }
}
