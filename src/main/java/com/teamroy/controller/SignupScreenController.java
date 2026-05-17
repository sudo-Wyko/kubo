package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.ConnectionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.sql.Connection;

public class SignupScreenController {

    @FXML
    private TextField contactField; // Updated to match the contact number lookup workflow
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleSignup() {
        String contactNumber = contactField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setText("");

        try {
            Connection conn = ConnectionManager.getConnection();
            try {
                SignupController signupHandler = new SignupController();
                signupHandler.handleRegistration(conn, contactNumber, username, password);
                
                errorLabel.setStyle("-fx-text-fill: #10b981;");
                errorLabel.setText("Account verified and created! Redirecting to login...");

                // Run delay background thread to maintain fluid UI responsiveness
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                        Platform.runLater(() -> {
                            try {
                                App.setRoot("login");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();

            } finally {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (IllegalArgumentException e) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                errorLabel.setText(e.getMessage());
            } else {
                errorLabel.setText("Registration failed: " + e.getMessage());
            }
            e.printStackTrace();
        } catch (Exception e) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setText("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSignupLink() {
        handleBack();
    }

    @FXML
    private void handleBack() {
        try {
            App.setRoot("login");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}