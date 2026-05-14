package com.teamroy.controller;
import com.teamroy.App;
import com.teamroy.ConnectionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.sql.Connection;
public class SignupScreenController {
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField emailField;
    @FXML
    private Label errorLabel;
    @FXML
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText() == null ? "" : emailField.getText().trim();
        errorLabel.setText("");
        try {
            Connection conn = ConnectionManager.getConnection();
            try {
                SignupController signupHandler = new SignupController();
                signupHandler.handleRegistration(conn, username, password, firstName, lastName, email);
                errorLabel.setStyle("-fx-text-fill: #10b981;");
                errorLabel.setText("Account created successfully! Redirecting to login...");
                javafx.application.Platform.runLater(() -> {
                    try {
                        Thread.sleep(1500);
                        App.setRoot("login");
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            } finally {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (IllegalArgumentException e) {
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            errorLabel.setText(e.getMessage());
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                errorLabel.setStyle("-fx-text-fill: #ef4444;");
                errorLabel.setText(e.getMessage());
            } else {
                errorLabel.setStyle("-fx-text-fill: #ef4444;");
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
