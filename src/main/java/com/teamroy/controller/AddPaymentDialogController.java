package com.teamroy.controller;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.PaymentService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
public class AddPaymentDialogController {
    @FXML
    private ComboBox<Tenant> tenantCombo;
    @FXML
    private Label tenantLockedLabel;
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
    private PaymentService paymentService;
    private boolean tenantSubmissionMode;
    private int lockedTenantId = -1;
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
    public void configureAdmin(java.sql.Connection conn, Runnable onSuccessCallback) {
        this.paymentService = new PaymentService(conn);
        this.tenantSubmissionMode = false;
        this.onSuccess = onSuccessCallback;
        tenantCombo.setDisable(false);
        tenantCombo.setVisible(true);
        tenantLockedLabel.setVisible(false);
        tenantLockedLabel.setManaged(false);
        statusCombo.setDisable(false);
        statusCombo.setVisible(true);
        statusLockedLabel.setVisible(false);
        statusLockedLabel.setManaged(false);
        statusCombo.getSelectionModel().selectFirst();
        bindTenantComboCells();
    }
    public void configureTenantSubmit(java.sql.Connection conn, Tenant tenant, double presetAmount,
            Runnable onSuccessCallback) {
        this.paymentService = new PaymentService(conn);
        this.tenantSubmissionMode = true;
        this.lockedTenantId = tenant.GetTenantID();
        this.onSuccess = onSuccessCallback;
        tenantCombo.setVisible(false);
        tenantCombo.setManaged(false);
        tenantLockedLabel.setVisible(true);
        tenantLockedLabel.setManaged(true);
        tenantLockedLabel.setText(tenant.GetFirstName() + " " + tenant.GetLastName());
        statusCombo.setVisible(false);
        statusCombo.setManaged(false);
        statusLockedLabel.setVisible(true);
        statusLockedLabel.setManaged(true);
        statusLockedLabel.setText("Status: PENDING");
        amountField.setText(presetAmount > 0 ? String.format("%.2f", presetAmount) : "");
        bindTenantComboCells();
    }
    public void setTenants(List<Tenant> tenants) {
        tenantCombo.setItems(FXCollections.observableArrayList(tenants));
        if (!tenantSubmissionMode && !tenants.isEmpty()) {
            tenantCombo.getSelectionModel().selectFirst();
        }
    }
    private void bindTenantComboCells() {
        tenantCombo.setCellFactory(lv -> new ListCell<Tenant>() {
            @Override
            protected void updateItem(Tenant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.GetFirstName() + " " + item.GetLastName());
                }
            }
        });
        tenantCombo.setButtonCell(new ListCell<Tenant>() {
            @Override
            protected void updateItem(Tenant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.GetFirstName() + " " + item.GetLastName());
                }
            }
        });
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
        Tenant selectedTenant = tenantSubmissionMode ? null : tenantCombo.getSelectionModel().getSelectedItem();
        if (!tenantSubmissionMode && selectedTenant == null) {
            showAmountError("Select a tenant.");
            return;
        }
        int tenantId = tenantSubmissionMode ? lockedTenantId : selectedTenant.GetTenantID();
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
        String status = tenantSubmissionMode ? "PENDING" : statusCombo.getSelectionModel().getSelectedItem();
        if (!tenantSubmissionMode && (status == null || status.isBlank())) {
            showAmountError("Select a status.");
            return;
        }
        LocalDateTime paymentDateTime = LocalDateTime.of(date, LocalTime.NOON);
        Payment payment = new Payment(tenantId, amount, paymentDateTime, method, status);
        paymentService.recordNewPayment(payment);
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
