package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.DocumentDaoImpl;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Document;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Tenant;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;

public class TenantLeaseController {

    @FXML
    private Label emptyLabel;
    @FXML
    private VBox leaseBox;
    @FXML
    private Label startLabel;
    @FXML
    private Label endLabel;
    @FXML
    private Label rentLabel;
    @FXML
    private Label statusBadge;

    private int tenantId = -1;

    private Connection getConnection() throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        }
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }

    @FXML
    private void initialize() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            showEmpty("No tenant session found.");
            return;
        }
        tenantId = tenant.GetTenantID();

        try (Connection conn = getConnection()) {
            LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
            List<Lease> leases = leaseDao.GetByTenantId(tenantId);
            Lease lease = leases.stream()
                    .filter(l -> l != null && "ACTIVE".equalsIgnoreCase(l.GetStatus()))
                    .findFirst()
                    .orElse(null);

            if (lease == null) {
                showEmpty("No active lease found.");
                return;
            }
            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            leaseBox.setVisible(true);
            leaseBox.setManaged(true);

            startLabel.setText("Start date: " + lease.GetStartDate());
            endLabel.setText("End date: " + lease.GetEndDate());
            rentLabel.setText("Monthly rent: " + CurrencyUtil.format(lease.GetMonthlyRent()));
            applyStatusBadge(lease.GetStatus());
        } catch (Exception ex) {
            ex.printStackTrace();
            showEmpty("Unable to load lease.");
        }
    }

    private void applyStatusBadge(String status) {
        statusBadge.setText(status == null ? "" : status);
        String bg = "#334155";
        if ("ACTIVE".equalsIgnoreCase(status)) {
            bg = "#15803d";
        } else if ("EXPIRED".equalsIgnoreCase(status)) {
            bg = "#475569";
        } else if ("TERMINATED".equalsIgnoreCase(status)) {
            bg = "#b91c1c";
        }
        statusBadge.setStyle("-fx-text-fill: white; -fx-padding: 4 12; -fx-background-radius: 10;"
                + " -fx-background-color: " + bg + ";");
    }

    private void showEmpty(String message) {
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
        leaseBox.setVisible(false);
        leaseBox.setManaged(false);
    }

    @FXML
    private void handleViewDocument() {
        if (tenantId < 0) {
            return;
        }

        try (Connection conn = getConnection()) {
            DocumentDaoImpl documentDao = new DocumentDaoImpl(conn);
            List<Document> docs = documentDao.GetByTenantID(tenantId);
            if (docs.isEmpty()) {
                missingDocAlert();
                return;
            }
            Document doc = docs.get(0);
            File file = new File(doc.GetFilePath());
            if (!file.exists()) {
                missingDocAlert();
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                missingDocAlert();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            missingDocAlert();
        }
    }

    private void missingDocAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Document unavailable");
        alert.setHeaderText(null);
        alert.setContentText("No document on file. Contact your administrator.");
        alert.showAndWait();
    }
}
