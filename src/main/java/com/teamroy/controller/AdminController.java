package com.teamroy.controller;

import com.teamroy.App;
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
    @FXML
    private Button btnLogout;

    private static final String ACTIVE_CLASS = "nav-button-active";

    @FXML
    private void initialize() {
        switchView("admin_dashboard.fxml", btnDashboard);
    }

    private void setActiveButton(Button activeButton) {
        List<Button> allButtons =
                Arrays.asList(btnDashboard, btnTenants, btnRooms, btnLeases, btnPayments, btnMaintenance);
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
            App.setRoot("login");
        } catch (IOException e) {
            System.err.println("Failed to navigate to login");
            e.printStackTrace();
        }
    }
}

