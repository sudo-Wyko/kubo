package com.teamroy.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.Connection;
import java.util.List;

import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.DatabaseUtility;
import com.teamroy.SessionManager;

public class TenantLeaseController {

    Connection conn = DatabaseUtility.getConnection();
    private LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
    private int currentTenantId;

    @FXML
    private Label lblStartDate;
    @FXML
    private Label lblEndDate;
    @FXML
    private Label lblStatus;

    // --- NEW: Label for Monthly Rent ---
    @FXML
    private Label lblMonthlyRent;

    @FXML
    public void initialize() {
        currentTenantId = SessionManager.getCurrentTenantId();
        loadLeaseData();
    }

    private void loadLeaseData() {
        // We will test by pulling the lease for the current Tenant
        List<Lease> leases = leaseDao.GetByTenantID(currentTenantId);

        // Assuming they have a lease, grab the most recent one to display
        if (leases != null && !leases.isEmpty()) {
            Lease activeLease = leases.get(0);

            // Format safely using your PascalCase getters
            String startDate = activeLease.GetStartDate() != null ? activeLease.GetStartDate().toString() : "N/A";
            String endDate = activeLease.GetEndDate() != null ? activeLease.GetEndDate().toString() : "N/A";
            String status = activeLease.GetStatus() != null ? activeLease.GetStatus() : "N/A";

            // --- NEW: Format the rent ---
            // Replace GetRentAmount() with whatever your exact getter name is in the Lease
            // entity!
            String monthlyRent = String.format("₱%,.2f", activeLease.GetMonthlyRent());

            // Update the UI labels
            lblStartDate.setText(startDate);
            lblEndDate.setText(endDate);
            lblStatus.setText(status.toUpperCase());
            lblMonthlyRent.setText(monthlyRent);

        } else {
            // If they don't have a lease in the database yet
            lblStartDate.setText("--/--/----");
            lblEndDate.setText("--/--/----");
            lblStatus.setText("NO LEASE");
            lblMonthlyRent.setText("₱0.00");
        }
    }
}