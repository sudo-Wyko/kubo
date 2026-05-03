package com.teamroy.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AdminController {

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

    private final String DEFAULT_STYLE =
            "-fx-background-color: transparent; -fx-text-fill: white; -fx-border-color: white; "
                    + "-fx-border-radius: 8; -fx-padding: 12; -fx-cursor: hand;";
    private final String ACTIVE_STYLE =
            "-fx-background-color: #333333; -fx-text-fill: white; -fx-border-color: white; "
                    + "-fx-border-radius: 8; -fx-padding: 12; -fx-cursor: hand;";

    @FXML
    private void initialize() {
        SwitchView("admin_dashboard.fxml", btnDashboard);
    }

    private void SetActiveButton(Button activeButton) {
        List<Button> allButtons =
                Arrays.asList(btnDashboard, btnTenants, btnRooms, btnLeases, btnPayments, btnMaintenance);
        for (Button btn : allButtons) {
            if (btn != null) {
                btn.setStyle(DEFAULT_STYLE);
            }
        }
        if (activeButton != null) {
            activeButton.setStyle(ACTIVE_STYLE);
        }
    }

    private void SwitchView(String fxmlFileName, Button targetButton) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/" + fxmlFileName));
            Parent view = loader.load();
            Object controller = loader.getController();
            if (controller instanceof AdminDashboardController) {
                ((AdminDashboardController) controller).setAdminController(this);
            }
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            SetActiveButton(targetButton);
        } catch (IOException e) {
            System.err.println("Could not load view: " + fxmlFileName);
            e.printStackTrace();
        }
    }

    @FXML
    void LoadDashboardView() {
        SwitchView("admin_dashboard.fxml", btnDashboard);
    }

    @FXML
    void LoadTenantsView() {
        SwitchView("admin_tenants.fxml", btnTenants);
    }

    @FXML
    void LoadRoomsView() {
        SwitchView("admin_rooms.fxml", btnRooms);
    }

    @FXML
    void LoadLeasesView() {
        SwitchView("admin_leases.fxml", btnLeases);
    }

    @FXML
    void LoadPaymentsView() {
        SwitchView("admin_payments.fxml", btnPayments);
    }

    @FXML
    void LoadMaintenanceView() {
        SwitchView("admin_maintenance.fxml", btnMaintenance);
    }
}
