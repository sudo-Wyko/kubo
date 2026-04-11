package com.teamroy.controller;

import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;
import javafx.scene.Parent;
import java.util.List;
import java.util.Arrays;
import java.io.IOException;

public class TenantController {

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

    private final String DEFAULT_STYLE = "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; -fx-border-radius: 8; -fx-padding: 12; -fx-cursor: hand;";
    private final String ACTIVE_STYLE = "-fx-background-color: #333333; -fx-text-fill: white; -fx-border-color: white; -fx-border-radius: 8; -fx-padding: 12; -fx-cursor: hand;";

    private void SetActiveButton(Button activeButton) {
        // Put all navigation buttons in a list
        List<Button> allButtons = Arrays.asList(btnHome, btnRooms, btnPayments, btnLease, btnMaintenance);

        // Reset all buttons to default
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle(DEFAULT_STYLE);
            }
        }

        // Highlight the clicked button
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }

    public void initialize() {
        // Automatically load the Home view and highlight the Home button on startup
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

    // --- Button Click Handlers ---

    @FXML
    private void LoadHomeView() {
        SwitchView("tenant_home.fxml", btnHome);
    }

    @FXML
    private void LoadRoomsView() {
        SwitchView("tenant_rooms.fxml", btnRooms);
    }

    @FXML
    private void LoadPaymentsView() {
        SwitchView("tenant_payments.fxml", btnPayments);
    }

    @FXML
    private void LoadLeaseView() {
        SwitchView("tenant_lease.fxml", btnLease);
    }

    @FXML
    private void LoadMaintenanceView() {
        SwitchView("tenant_maintenance.fxml", btnMaintenance);
    }

    @FXML
    private void LoadAccountView() {
        // You might want to load an account FXML, or use App.setRoot("login") to log
        // out
        System.out.println("Account clicked");
    }
}