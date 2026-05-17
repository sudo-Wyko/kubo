package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.ConnectionManager;
import com.teamroy.SessionManager;
import com.teamroy.PasswordUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class AdminController {
    private Connection conn = ConnectionManager.getConnection();

    @FXML
    private StackPane contentArea;
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnTenants;
    @FXML
    private Button btnRooms;
    @FXML
    private Button btnLeases;
    @FXML
    private Button btnPayments;
    @FXML
    private Button btnMaintenance;
    @FXML
    private Button btnAccount; // Linked to your new FXML button
    @FXML
    private Button btnLogout;

    private static final String ACTIVE_CLASS = "nav-button-active";

    @FXML
    private void initialize() {
        switchView("admin_dashboard.fxml", btnDashboard);
    }

    private void setActiveButton(Button activeButton) {
        // Added btnAccount to this list so it deselects other tabs when clicked
        List<Button> allButtons =
                Arrays.asList(btnDashboard, btnTenants, btnRooms, btnLeases, btnPayments, btnMaintenance, btnAccount);
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove(ACTIVE_CLASS);
            }
        }
        if (activeButton != null) {
            if (!activeButton.getStyleClass().contains(ACTIVE_CLASS)) {
                activeButton.getStyleClass().add(ACTIVE_CLASS);
            }
        }
    }

    private void switchView(String fxmlFileName, Button targetButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/" + fxmlFileName));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).setAdminController(this);
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            setActiveButton(targetButton);
        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlFileName);
            e.printStackTrace();
        }
    }

    @FXML
    private void loadDashboardView() {
        switchView("admin_dashboard.fxml", btnDashboard);
    }

    @FXML
    private void loadTenantsView() {
        switchView("admin_tenants.fxml", btnTenants);
    }

    @FXML
    private void loadRoomsView() {
        switchView("admin_rooms.fxml", btnRooms);
    }

    @FXML
    private void loadLeasesView() {
        switchView("admin_leases.fxml", btnLeases);
    }

    @FXML
    private void loadPaymentsView() {
        switchView("admin_payments.fxml", btnPayments);
    }

    @FXML
    public void loadMaintenanceView() {
        switchView("admin_maintenance.fxml", btnMaintenance);
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.logout(); // Clears user session data safely
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Failed to navigate to login");
            e.printStackTrace();
        }
    }

    @FXML
    public void LoadAccountView() {
        setActiveButton(btnAccount);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Account Settings");
        dialog.setHeaderText("Update your credentials below.\nNote: Current password is required to save changes.");
        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");

        TextField usernameField = new TextField();
        usernameField.setPromptText("New Username (Optional)");

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Required to save changes");
        
        // Reordered layout alignment
        grid.add(new Label("New Password:"), 0, 0);
        grid.add(newPasswordField, 1, 0);

        grid.add(new Label("New Username (Optional):"), 0, 1);
        grid.add(usernameField, 1, 1);

        grid.add(new Label("Current Password * :"), 0, 2);
        grid.add(currentPasswordField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                processAccountUpdate(
                        usernameField.getText().trim(),
                        currentPasswordField.getText(),
                        newPasswordField.getText());
            }
        });
    }

    private void processAccountUpdate(String newUser, String currentPass, String newPass) {
    int userId = SessionManager.getCurrentUserId();
    
    if (currentPass.isEmpty()) {
        showAlert(Alert.AlertType.WARNING, "Security Requirement", "You must enter your current password to save changes.");
        return;
    }
    
    if (newUser.isEmpty() && newPass.isEmpty()) {
        showAlert(Alert.AlertType.INFORMATION, "No Changes", "No new credentials were typed.");
        return;
    }

    String verifySql = "SELECT password_hash, username FROM USER_ACCOUNT WHERE user_id = ?";
    
    try (Connection conn = ConnectionManager.getConnection();
         PreparedStatement verifyPs = conn.prepareStatement(verifySql)) {
        
        verifyPs.setInt(1, userId);
        try (ResultSet rs = verifyPs.executeQuery()) {
            if (rs.next()) {
                String dbPasswordHash = rs.getString("password_hash");
                String currentUsername = rs.getString("username");
                
                // Hash the user's plain-text input to compare against the DB hash
                String hashedCurrentInput = PasswordUtil.hash(currentPass);
                
                if (dbPasswordHash.equals(hashedCurrentInput)) {
                    
                    String finalUsername = newUser.isEmpty() ? currentUsername : newUser;
                    
                    // Hash the new password if provided; otherwise, keep the existing hash
                    String finalPassword = newPass.isEmpty() ? dbPasswordHash : PasswordUtil.hash(newPass);
                    
                    String updateSql = "UPDATE USER_ACCOUNT SET username = ?, password_hash = ? WHERE user_id = ?";
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, finalUsername);
                        updatePs.setString(2, finalPassword);
                        updatePs.setInt(3, userId);
                        
                        int affectedRows = updatePs.executeUpdate();
                        if (affectedRows > 0) {
                            showAlert(Alert.AlertType.INFORMATION, "Success", "Account credentials updated successfully!");
                        }
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Security Error", "The current password you entered is incorrect.");
                }
            }
        }
    } catch (SQLException e) {
        showAlert(Alert.AlertType.ERROR, "Database Error", "An error occurred while updating the account.");
        e.printStackTrace();
    }
}

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}