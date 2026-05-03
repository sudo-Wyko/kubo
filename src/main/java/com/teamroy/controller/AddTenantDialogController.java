package com.teamroy.controller;

import com.teamroy.PasswordUtil;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.dao.UserAccountDaoImpl;
import com.teamroy.model.entity.Tenant;
import com.teamroy.model.entity.UserAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;

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

    private Connection conn;
    private TenantDaoImpl tenantDao;
    private UserAccountDaoImpl userDao;
    private Tenant editingTenant;
    private Runnable onSuccess;

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    public void configureCreate(Connection connection, Runnable onSuccessCallback) {
        this.conn = connection;
        this.tenantDao = new TenantDaoImpl(connection);
        this.userDao = new UserAccountDaoImpl(connection);
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
        this.conn = connection;
        this.tenantDao = new TenantDaoImpl(connection);
        this.userDao = new UserAccountDaoImpl(connection);
        this.editingTenant = tenant;
        this.onSuccess = onSuccessCallback;

        titleLabel.setText("Edit tenant");
        firstNameField.setText(tenant.GetFirstName());
        lastNameField.setText(tenant.GetLastName());
        contactField.setText(tenant.GetContactNumber() == null ? "" : tenant.GetContactNumber());
        emailField.setText(tenant.GetEmail() == null ? "" : tenant.GetEmail());
        usernameField.setDisable(true);
        passwordField.setDisable(true);

        Integer userId = tenant.GetUserID();
        if (userId != null) {
            UserAccount acct = userDao.GetByID(userId);
            if (acct != null) {
                usernameField.setText(acct.GetUsername());
            }
        }
    }

    @FXML
    private void save() {
        clearError();

        String firstName = safe(firstNameField.getText());
        String lastName = safe(lastNameField.getText());
        if (firstName.isBlank() || lastName.isBlank()) {
            showError("First and last name are required.");
            return;
        }

        String contact = safe(contactField.getText());
        String email = safe(emailField.getText());
        String username = safe(usernameField.getText());
        String password = passwordField.getText();

        if (editingTenant == null) {
            Integer userIdLink = null;
            if (!username.isBlank()) {
                if (password == null || password.isBlank()) {
                    showError("Password is required when username is provided.");
                    return;
                }
                if (userDao.GetByUsername(username) != null) {
                    showError("Username already exists.");
                    return;
                }

                UserAccount account = new UserAccount(username, PasswordUtil.hash(password), "TENANT");
                userDao.Create(account);
                userIdLink = account.GetUserID();
            }

            Tenant tenant = new Tenant(firstName, lastName, contact, email, 0.0);
            tenant.SetUserID(userIdLink);
            tenantDao.Create(tenant);
        } else {
            editingTenant.SetFirstName(firstName);
            editingTenant.SetLastName(lastName);
            editingTenant.SetContactNumber(contact);
            editingTenant.SetEmail(email);
            tenantDao.Update(editingTenant);
        }

        if (onSuccess != null) {
            onSuccess.run();
        }

        cancel();
    }

    private static String safe(String text) {
        return text == null ? "" : text.trim();
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
