package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.ConnectionManager;
import com.teamroy.PasswordUtil;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.dao.UserAccountDaoImpl;
import com.teamroy.model.entity.Tenant;
import com.teamroy.model.entity.UserAccount;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoginController {

    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;

    @FXML
    private void handleSignupLink() {
        try {
            App.setRoot("signup");
        } catch (IOException e) {
            errorLabel.setText("Could not load signup page.");
            LOGGER.log(Level.SEVERE, "Failed to load signup view.", e);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            errorLabel.setText("Please enter credentials.");
            return;
        }

        SessionManager.clear();

        try {
            // Use ConnectionManager which handles automatic database initialization
            Connection conn = ConnectionManager.getConnection();
            try {
                UserAccountDaoImpl userDao = new UserAccountDaoImpl(conn);
                UserAccount user = userDao.GetByUsername(username);

                if (user != null) {
                    String hashedPassword = PasswordUtil.hash(password);
                    String storedPassword = user.GetPassword();

                    if (hashedPassword.equals(storedPassword)) {
                        SessionManager.setCurrentUser(user);

                        String role = user.GetRole();

                        if ("TENANT".equalsIgnoreCase(role)) {
                            TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
                            Tenant tenant = tenantDao.GetByUserID(user.GetUserID());
                            SessionManager.setCurrentTenant(tenant);
                        } else {
                            SessionManager.setCurrentTenant(null);
                        }

                        if ("ADMIN".equalsIgnoreCase(role)) {
                            App.setRoot("admin");
                        } else {
                            App.setRoot("tenant");
                        }
                    } else {
                        errorLabel.setText("Invalid username or password.");
                    }
                } else {
                    errorLabel.setText("Invalid username or password.");
                }
            } finally {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("Failed to connect to database")) {
                errorLabel.setText("Database error: Could not initialize database. Check your MySQL server.");
            } else {
                errorLabel.setText("Database error: " + e.getMessage());
            }
            LOGGER.log(Level.SEVERE, "Login failed due to database runtime error.", e);
        } catch (Exception e) {
            errorLabel.setText("An unexpected error occurred: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Login failed unexpectedly.", e);
        }
    }
}