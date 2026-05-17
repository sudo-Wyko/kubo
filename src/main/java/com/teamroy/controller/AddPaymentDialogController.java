package com.teamroy.controller;

import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddPaymentDialogController {
    @FXML
    private ComboBox<Lease> leaseCombo;
    @FXML
    private Label leaseLockedLabel;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker paymentDatePicker;
    @FXML
    private ComboBox<String> methodCombo;
    @FXML
    private ComboBox<String> statusCombo;
    @FXML
    private Label statusLockedLabel;
    @FXML
    private Label amountErrorLabel;
    @FXML
    private Button saveButton;

    private PaymentDaoImpl paymentDao;
    private LeaseDaoImpl leaseDao;
    private TenantDaoImpl tenantDao;
    private final Map<Integer, Tenant> tenantCache = new HashMap<>();
    private boolean tenantSubmissionMode;
    private int lockedLeaseId = -1;
    private Runnable onSuccess;

    @FXML
    private void initialize() {
        methodCombo.setItems(FXCollections.observableArrayList("Cash", "GCash", "Bank Transfer"));
        methodCombo.getSelectionModel().selectFirst();
        statusCombo.setItems(FXCollections.observableArrayList("PENDING", "VERIFIED", "FAILED"));
        paymentDatePicker.setValue(LocalDate.now());
        amountErrorLabel.setManaged(false);
        amountErrorLabel.setVisible(false);
    }

    public void configureAdmin(Connection conn, Runnable onSuccessCallback) {
        this.paymentDao = new PaymentDaoImpl(conn);
        this.leaseDao = new LeaseDaoImpl(conn);
        this.tenantDao = new TenantDaoImpl(conn);
        this.tenantSubmissionMode = false;
        this.onSuccess = onSuccessCallback;
        tenantCache.clear();

        leaseCombo.setDisable(false);
        leaseCombo.setVisible(true);
        leaseLockedLabel.setVisible(false);
        leaseLockedLabel.setManaged(false);
        statusCombo.setDisable(false);
        statusCombo.setVisible(true);
        statusLockedLabel.setVisible(false);
        statusLockedLabel.setManaged(false);
        statusCombo.getSelectionModel().select("VERIFIED");
        bindLeaseComboCells();
    }

    public void configureTenantSubmit(Connection conn, Tenant tenant, double presetAmount, Runnable onSuccessCallback) {
        this.paymentDao = new PaymentDaoImpl(conn);
        this.leaseDao = new LeaseDaoImpl(conn);
        this.tenantDao = new TenantDaoImpl(conn);
        this.tenantSubmissionMode = true;
        this.onSuccess = onSuccessCallback;
        tenantCache.clear();

        Lease activeLease = leaseDao.GetActiveLeaseByTenant(tenant.GetTenantID());
        if (activeLease != null) {
            lockedLeaseId = activeLease.GetLeaseID();
            tenantCache.put(tenant.GetTenantID(), tenant);
        }

        leaseCombo.setVisible(false);
        leaseCombo.setManaged(false);
        leaseLockedLabel.setVisible(true);
        leaseLockedLabel.setManaged(true);
        leaseLockedLabel.setText(formatLeaseLabel(activeLease));

        statusCombo.setVisible(false);
        statusCombo.setManaged(false);
        statusLockedLabel.setVisible(true);
        statusLockedLabel.setManaged(true);
        statusLockedLabel.setText("Status: PENDING");
        amountField.setText(presetAmount > 0 ? String.format("%.2f", presetAmount) : "");
        bindLeaseComboCells();
    }

    public void setLeases(List<Lease> leases) {
        tenantCache.clear();
        for (Lease lease : leases) {
            tenantCache.computeIfAbsent(lease.GetTenantID(), tenantDao::GetByID);
        }
        leaseCombo.setItems(FXCollections.observableArrayList(leases));
        if (!tenantSubmissionMode && !leases.isEmpty()) {
            leaseCombo.getSelectionModel().selectFirst();
        }
    }

    private void bindLeaseComboCells() {
        leaseCombo.setCellFactory(lv -> new ListCell<Lease>() {
            @Override
            protected void updateItem(Lease item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatLeaseLabel(item));
            }
        });
        leaseCombo.setButtonCell(new ListCell<Lease>() {
            @Override
            protected void updateItem(Lease item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatLeaseLabel(item));
            }
        });
    }

    private String formatLeaseLabel(Lease lease) {
        if (lease == null) {
            return "No active lease";
        }
        return tenantDisplayName(lease.GetTenantID()) + " - Lease #" + lease.GetLeaseID();
    }

    private String tenantDisplayName(int tenantId) {
        Tenant tenant = tenantCache.computeIfAbsent(tenantId, tenantDao::GetByID);
        if (tenant == null) {
            return "Unknown Tenant";
        }
        String first = tenant.GetFirstName() == null ? "" : tenant.GetFirstName().trim();
        String last = tenant.GetLastName() == null ? "" : tenant.GetLastName().trim();
        String full = (first + " " + last).trim();
        return full.isBlank() ? ("Tenant #" + tenantId) : full;
    }

    @FXML
    private void confirm() {
        amountErrorLabel.setVisible(false);
        amountErrorLabel.setManaged(false);

        String rawAmount = amountField.getText();
        if (rawAmount == null || rawAmount.isBlank()) {
            showAmountError("Amount is required.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(rawAmount.trim());
        } catch (NumberFormatException ex) {
            showAmountError("Enter a valid positive number.");
            return;
        }
        if (amount <= 0) {
            showAmountError("Amount must be positive.");
            return;
        }

        int leaseId;
        if (tenantSubmissionMode) {
            if (lockedLeaseId < 0) {
                showAmountError("No active lease found for this tenant.");
                return;
            }
            leaseId = lockedLeaseId;
        } else {
            Lease selectedLease = leaseCombo.getSelectionModel().getSelectedItem();
            if (selectedLease == null) {
                showAmountError("Select a lease.");
                return;
            }
            leaseId = selectedLease.GetLeaseID();
        }

        LocalDate date = paymentDatePicker.getValue();
        if (date == null) {
            showAmountError("Pick a payment date.");
            return;
        }
        String method = methodCombo.getSelectionModel().getSelectedItem();
        if (method == null || method.isBlank()) {
            showAmountError("Select a payment method.");
            return;
        }

        String status;
        if (tenantSubmissionMode) {
            status = "PENDING";
        } else {
            status = statusCombo.getSelectionModel().getSelectedItem();
            if (status == null || status.isBlank()) {
                status = "VERIFIED";
            }
        }

        Payment payment = new Payment(leaseId, amount, LocalDateTime.of(date, LocalTime.NOON), method, status);
        paymentDao.Create(payment);
        if (onSuccess != null) {
            onSuccess.run();
        }
        close();
    }

    private void showAmountError(String message) {
        amountErrorLabel.setText(message);
        amountErrorLabel.setVisible(true);
        amountErrorLabel.setManaged(true);
    }

    @FXML
    private void cancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
