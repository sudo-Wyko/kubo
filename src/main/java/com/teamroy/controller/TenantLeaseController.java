package com.teamroy.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.Connection;
import java.util.List;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.ConnectionManager;
import com.teamroy.SessionManager;
public class TenantLeaseController {
    Connection conn = ConnectionManager.getConnection();
    private LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
    private int currentTenantId;
    @FXML
    private Label lblStartDate;
    @FXML
    private Label lblEndDate;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblMonthlyRent;
    @FXML
    public void initialize() {
        currentTenantId = SessionManager.getCurrentTenantId();
        loadLeaseData();
    }
    private void loadLeaseData() {
        List<Lease> leases = leaseDao.GetByTenantId(currentTenantId);
        if (leases != null && !leases.isEmpty()) {
            Lease activeLease = leases.get(0);
            String startDate = activeLease.GetStartDate() != null ? activeLease.GetStartDate().toString() : "N/A";
            String endDate = activeLease.GetEndDate() != null ? activeLease.GetEndDate().toString() : "N/A";
            String status = activeLease.GetStatus() != null ? activeLease.GetStatus() : "N/A";
            String monthlyRent = String.format("\u20b1%,.2f", activeLease.GetMonthlyRent());
            lblStartDate.setText(startDate);
            lblEndDate.setText(endDate);
            lblStatus.setText(status.toUpperCase());
            lblMonthlyRent.setText(monthlyRent);
        } else {
            lblStartDate.setText("--/--/----");
            lblEndDate.setText("--/--/----");
            lblStatus.setText("NO LEASE");
            lblMonthlyRent.setText("\u20b10.00");
        }
    }
}
