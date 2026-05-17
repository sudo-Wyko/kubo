package com.teamroy.controller;

import com.teamroy.model.dao.DaoException;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.ChargeService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddChargeDialogController {
    @FXML
    private ComboBox<Lease> leaseCombo;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField amountField;
    @FXML
    private DatePicker dueDatePicker;
    @FXML
    private TextField descriptionField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private ChargeService chargeService;
    private RoomDaoImpl roomDao;
    private TenantDaoImpl tenantDao;
    private final Map<Integer, Tenant> tenantCache = new HashMap<>();
    private Runnable onSuccess;

    @FXML
    private void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList("RENT", "LATE_FEE", "DEPOSIT", "UTILITY"));
        typeCombo.getSelectionModel().selectFirst();
        dueDatePicker.setValue(LocalDate.now().plusDays(7));
    }

    public void configure(Connection conn, Runnable onSuccessCallback) {
        this.chargeService = new ChargeService(conn);
        this.roomDao = new RoomDaoImpl(conn);
        this.tenantDao = new TenantDaoImpl(conn);
        this.onSuccess = onSuccessCallback;
        tenantCache.clear();
        bindLeaseCells();
    }

    public void setLeases(List<Lease> leases) {
        tenantCache.clear();
        for (Lease lease : leases) {
            tenantCache.computeIfAbsent(lease.GetTenantID(), tenantDao::GetByID);
        }
        leaseCombo.setItems(FXCollections.observableArrayList(leases));
        if (!leases.isEmpty()) {
            leaseCombo.getSelectionModel().selectFirst();
        }
    }

    private void bindLeaseCells() {
        leaseCombo.setCellFactory(lv -> new ListCell<Lease>() {
            @Override
            protected void updateItem(Lease item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatLease(item));
            }
        });
        leaseCombo.setButtonCell(new ListCell<Lease>() {
            @Override
            protected void updateItem(Lease item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatLease(item));
            }
        });
    }

    private String formatLease(Lease lease) {
        String tenantName = tenantDisplayName(lease.GetTenantID());
        Room room = roomDao.GetByID(lease.GetRoomID());
        String roomNumber = room == null ? ("#" + lease.GetRoomID()) : room.GetRoomNumber();
        return tenantName + " - " + roomNumber + " - "
                + String.format("₱%,.2f", lease.GetBalance());
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
        clearError();
        Lease lease = leaseCombo.getSelectionModel().getSelectedItem();
        if (lease == null) {
            showError("Select a lease.");
            return;
        }
        String type = typeCombo.getSelectionModel().getSelectedItem();
        if (type == null || type.isBlank()) {
            showError("Select a charge type.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountField.getText().trim());
        } catch (NumberFormatException ex) {
            showError("Enter a valid positive amount.");
            return;
        }
        if (amount <= 0) {
            showError("Amount must be greater than zero.");
            return;
        }
        LocalDate dueDate = dueDatePicker.getValue();
        if (dueDate == null) {
            showError("Pick a due date.");
            return;
        }
        try {
            chargeService.issueChargeToLease(
                    lease.GetLeaseID(),
                    type,
                    amount,
                    dueDate,
                    descriptionField.getText());
            if (onSuccess != null) {
                onSuccess.run();
            }
            close();
        } catch (DaoException ex) {
            showError(ex.getMessage());
        }
    }

    @FXML
    private void cancel() {
        close();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    private void close() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
