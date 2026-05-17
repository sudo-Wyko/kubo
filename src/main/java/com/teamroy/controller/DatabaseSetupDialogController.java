package com.teamroy.controller;

import com.teamroy.DatabaseConfig;
import com.teamroy.DatabaseInitializer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DatabaseSetupDialogController {
    @FXML
    private TextField hostField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private TextField databaseField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button connectButton;

    private boolean completed;
    private boolean cancelled;

    @FXML
    private void initialize() {
        hostField.setText(DatabaseConfig.DEFAULT_HOST);
        usernameField.setText(DatabaseConfig.DEFAULT_USER);
        databaseField.setText("kubo");
    }

    @FXML
    private void connect() {
        clearError();
        String host = trim(hostField.getText());
        String user = trim(usernameField.getText());
        String pass = passwordField.getText() == null ? "" : passwordField.getText();
        String dbName = trim(databaseField.getText());

        if (host.isBlank()) {
            showError("Host is required.");
            return;
        }
        if (user.isBlank()) {
            showError("Username is required.");
            return;
        }
        if (dbName.isBlank()) {
            showError("Database name is required.");
            return;
        }

        connectButton.setDisable(true);
        try {
            DatabaseInitializer.setupDatabase(host, dbName, user, pass);
            DatabaseConfig.save(host, dbName, user, pass);
            completed = true;
            close();
        } catch (Exception ex) {
            showError(ex.getMessage() == null ? "Database setup failed." : ex.getMessage());
        } finally {
            connectButton.setDisable(false);
        }
    }

    @FXML
    private void cancel() {
        cancelled = true;
        close();
    }

    public boolean wasCompleted() {
        return completed;
    }

    public boolean wasCancelled() {
        return cancelled;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void close() {
        Stage stage = (Stage) connectButton.getScene().getWindow();
        stage.close();
    }
}
