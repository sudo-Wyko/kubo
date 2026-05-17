package com.teamroy.controller;

import com.teamroy.PhInputValidator;
import com.teamroy.model.dao.DaoException;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.TenantService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.util.Optional;

public class AddTenantDialogController {
    @FXML
    private Label titleLabel;
    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField contactField;
    @FXML
    private TextField emailField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private TenantService tenantService;
    private Tenant editingTenant;
    private Runnable onSuccess;

    public void configureCreate(Connection connection, Runnable onSuccessCallback) {
        this.tenantService = new TenantService(connection);
        this.editingTenant = null;
        this.onSuccess = onSuccessCallback;
        titleLabel.setText("Add tenant");
        firstNameField.clear();
        lastNameField.clear();
        contactField.clear();
        emailField.clear();
        usernameField.setDisable(false);
        passwordField.setDisable(false);
        usernameField.clear();
        passwordField.clear();
        clearError();
    }

    public void configureEdit(Connection connection, Tenant tenant, Runnable onSuccessCallback) {
        this.tenantService = new TenantService(connection);
        this.editingTenant = tenant;
        this.onSuccess = onSuccessCallback;
        titleLabel.setText("Edit tenant");
        firstNameField.setText(tenant.GetFirstName());
        lastNameField.setText(tenant.GetLastName());
        contactField.setText(tenant.GetContactNumber() == null ? "" : tenant.GetContactNumber());
        emailField.setText(tenant.GetEmail() == null ? "" : tenant.GetEmail());
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        clearError();
    }

    @FXML
    private void save() {
        clearError();
        String firstName = safe(firstNameField.getText());
        String lastName = safe(lastNameField.getText());
        if (firstName.isBlank() || lastName.isBlank()) {
            validationAlert("First and last name are required.");
            return;
        }

        String contactRaw = safe(contactField.getText());
        Optional<String> contactError = PhInputValidator.validateContactRequired(contactRaw);
        if (contactError.isPresent()) {
            validationAlert(contactError.get());
            return;
        }
        String contact = PhInputValidator.normalizePhone(contactRaw);

        String email = safe(emailField.getText());
        Optional<String> emailError = PhInputValidator.validateEmailOptional(email);
        if (emailError.isPresent()) {
            validationAlert(emailError.get());
            return;
        }

        String username = safe(usernameField.getText());
        String password = passwordField.getText();

        try {
            if (editingTenant == null) {
                tenantService.createTenant(firstName, lastName, contact, email, username, password);
            } else {
                editingTenant.SetFirstName(firstName);
                editingTenant.SetLastName(lastName);
                editingTenant.SetContactNumber(contact);
                editingTenant.SetEmail(email.isBlank() ? null : email);
                tenantService.updateTenant(editingTenant);
            }
            if (onSuccess != null) {
                onSuccess.run();
            }
            close();
        } catch (DaoException ex) {
            showError(ex.getMessage());
        }
    }

    private void validationAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid input");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
