package com.teamroy.controller;

import com.teamroy.App;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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

        SessionManager.clear();

        try (Connection conn = getConnection()) {
            UserAccountDaoImpl userDao = new UserAccountDaoImpl(conn);
            UserAccount user = userDao.GetByUsername(username);
            String hashedPassword = PasswordUtil.hash(password);

            if (user != null && hashedPassword.equals(user.GetPassword())) {
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
        } catch (Exception e) {
            errorLabel.setText("Database error: Check config.properties");
            e.printStackTrace();
        }
    }
}