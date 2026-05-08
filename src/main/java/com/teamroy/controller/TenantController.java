package com.teamroy.controller;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
import com.teamroy.SessionManager;

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

public class TenantController {
    private Connection conn = DatabaseUtility.getConnection();
    private static TenantController instance;

    @FXML
    private StackPane contentArea;

    @FXML
    private Button btnHome;
    @FXML
    private Button btnRooms;
    @FXML
    private Button btnPayments;
    @FXML
    private Button btnLease;
    @FXML
    private Button btnMaintenance;

    private void SetActiveButton(Button activeButton) {
        List<Button> allButtons = Arrays.asList(btnHome, btnRooms, btnPayments, btnLease, btnMaintenance);

        for (Button btn : allButtons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button"))
                    btn.getStyleClass().add("nav-button");
            }
        }

        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button");
            if (!activeButton.getStyleClass().contains("nav-button-active"))
                activeButton.getStyleClass().add("nav-button-active");
        }
    }

    public void initialize() {
        instance = this;
        SwitchView("tenant_home.fxml", btnHome);
    }

    // --- View Switcher Logic ---
    private void SwitchView(String fxmlFileName, Button targetButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/" + fxmlFileName));
            Parent view = loader.load();

            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            SetActiveButton(targetButton);

        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlFileName);
            e.printStackTrace();
        }
    }

    public static TenantController getInstance() {
        return instance;
    }

    @FXML
    private void handleLogout() {
        try {
            SessionManager.logout();
            App.setRoot("login");
            System.out.println("User logged out successfully.");
        } catch (IOException e) {
            System.err.println("Could not switch to login screen.");
            e.printStackTrace();
        }
    }

    private void processAccountUpdate(String newUser, String currentPass, String newPass) {
        int userId = SessionManager.getCurrentUserId();

        if (newUser.isEmpty() || currentPass.isEmpty() || newPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Form Incomplete", "Please fill in all fields.");
            return;
        }

        String verifySql = "SELECT password_hash FROM USER_ACCOUNT WHERE user_id = ?";
        String updateSql = "UPDATE USER_ACCOUNT SET username = ?, password_hash = ? WHERE user_id = ?";

        try (PreparedStatement verifyPs = conn.prepareStatement(verifySql)) {
            verifyPs.setInt(1, userId);
            ResultSet rs = verifyPs.executeQuery();

            if (rs.next()) {
                String dbPasswordHash = rs.getString("password_hash");

                if (dbPasswordHash.equals(currentPass)) {
                    try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                        updatePs.setString(1, newUser);
                        updatePs.setString(2, newPass);
                        updatePs.setInt(3, userId);

                        int affectedRows = updatePs.executeUpdate();
                        if (affectedRows > 0) {
                            showAlert(Alert.AlertType.INFORMATION, "Success",
                                    "Account credentials updated successfully!");
                        }
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Security Error",
                            "The current password you entered is incorrect.");
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

    // --- Button Click Handlers ---

    @FXML
    public void LoadHomeView() {
        SwitchView("tenant_home.fxml", btnHome);
    }

    @FXML
    public void LoadRoomsView() {
        SwitchView("tenant_rooms.fxml", btnRooms);
    }

    @FXML
    public void LoadPaymentsView() {
        SwitchView("tenant_payments.fxml", btnPayments);
    }

    @FXML
    public void LoadLeaseView() {
        SwitchView("tenant_lease.fxml", btnLease);
    }

    @FXML
    public void LoadMaintenanceView() {
        SwitchView("tenant_maintenance.fxml", btnMaintenance);
    }

    @FXML
    public void LoadAccountView() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Account Settings");
        dialog.setHeaderText("Update your credentials below.");

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(15);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("New Username");
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current Password");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");

        grid.add(new Label("New Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Current Password:"), 0, 1);
        grid.add(currentPasswordField, 1, 1);
        grid.add(new Label("New Password:"), 0, 2);
        grid.add(newPasswordField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                processAccountUpdate(
                        usernameField.getText(),
                        currentPasswordField.getText(),
                        newPasswordField.getText());
            }
        });
    }
}