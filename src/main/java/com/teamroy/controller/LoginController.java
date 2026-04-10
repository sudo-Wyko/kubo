package com.teamroy.controller;

import com.teamroy.App;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class LoginController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    // Use a helper method to get the connection
    private Connection getConnection() throws Exception {
        Properties props = new Properties();
        // This looks for the file in your project root
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        } catch (IOException e) {
            throw new Exception("Could not find config.properties file!");
        }

        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter credentials.");
            return;
        }

        try (Connection conn = getConnection()) {
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
            errorLabel.setText("Database error: Check config.properties");
            e.printStackTrace();
        }
    }
}