package com.teamroy.controller;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.scene.shape.SVGPath;
import javafx.application.Platform;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.sql.Connection;
import com.teamroy.ConnectionManager;
import com.teamroy.SessionManager;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.dao.PaymentDaoImpl;
public class TenantPaymentsController {
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private FlowPane paymentGrid;
    @FXML
    private Button addPaymentBtn;
    private Connection conn = ConnectionManager.getConnection();
    private PaymentDaoImpl paymentDao = new PaymentDaoImpl(conn);
    private int currentTenantId;
    @FXML
    public void initialize() {
        currentTenantId = SessionManager.getCurrentTenantId();
        statusFilter.getItems().addAll("All", "PENDING", "VERIFIED");
        statusFilter.setValue("All");
        statusFilter.getStyleClass().add("payments-filter");
        addPaymentBtn.getStyleClass().add("payments-add-btn");
        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadPaymentHistory();
        });
        loadPaymentHistory();
    }
    private void loadPaymentHistory() {
        paymentGrid.getChildren().clear();
        List<Payment> payments = paymentDao.GetByTenantID(currentTenantId);
        String currentFilter = statusFilter.getValue();
        for (Payment payment : payments) {
            String status = payment.GetStatus() != null ? payment.GetStatus() : "PENDING";
            if (!"All".equalsIgnoreCase(currentFilter) && !status.equalsIgnoreCase(currentFilter)) {
                continue;
            }
            String paymentId = "P-" + payment.GetPaymentID();
            String tenantInfo = "Tenant ID: " + payment.GetTenantID();
            String amount = String.format("%,.2f", payment.GetAmountPaid());
            String date = payment.GetPaymentDate() != null ? payment.GetPaymentDate().toLocalDate().toString() : "N/A";
            String method = payment.GetPaymentMethod() != null ? payment.GetPaymentMethod() : "N/A";
            VBox card = createReceiptCard(paymentId, tenantInfo, amount, date, method, status);
            paymentGrid.getChildren().add(card);
        }
    }
    @FXML
    private void handleAddPayment() {
        Dialog<Payment> dialog = new Dialog<>();
        dialog.setTitle("Add New Payment");
        dialog.setHeaderText("Enter your payment details");
        ButtonType saveButtonType = new ButtonType("Submit Payment", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        TextField amountField = new TextField();
        amountField.setPromptText("e.g., 4500.00");
        ComboBox<String> methodBox = new ComboBox<>();
        methodBox.getItems().addAll("Cash", "GCash", "Bank Transfer");
        methodBox.setValue("GCash");
        grid.add(new Label("Amount (\u20b1):"), 0, 0);
        grid.add(amountField, 1, 0);
        grid.add(new Label("Method:"), 0, 1);
        grid.add(methodBox, 1, 1);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(amountField::requestFocus);
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    double amount = Double.parseDouble(amountField.getText());
                    Payment newPayment = new Payment();
                    newPayment.SetTenantID(currentTenantId);
                    newPayment.SetAmountPaid(amount);
                    newPayment.SetPaymentDate(LocalDateTime.now());
                    newPayment.SetPaymentMethod(methodBox.getValue());
                    newPayment.SetStatus("PENDING");
                    return newPayment;
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter a valid numeric amount.");
                    alert.showAndWait();
                    return null;
                }
            }
            return null;
        });
        Optional<Payment> result = dialog.showAndWait();
        result.ifPresent(payment -> {
            paymentDao.Create(payment);
            loadPaymentHistory();
        });
    }
    private VBox createReceiptCard(String id, String tenant, String amount, String date, String method, String status) {
        VBox receipt = new VBox();
        receipt.setPrefWidth(220);
        VBox cardBody = new VBox();
        cardBody.getStyleClass().add("receipt-card");
        Region accentBar = new Region();
        accentBar.setPrefHeight(6);
        if (status.equalsIgnoreCase("VERIFIED")) {
            accentBar.getStyleClass().add("receipt-accent-verfied");
        } else if (status.equalsIgnoreCase("PENDING")) {
            accentBar.getStyleClass().add("receipt-accent-pending");
        } else {
            accentBar.getStyleClass().add("receipt-accent-failed");
        }
        VBox content = new VBox(12);
        content.setPadding(new Insets(16, 15, 16, 15));
        content.getChildren().addAll(
                createDataRow("Payment ID", id),
                createDataRow("Tenant", tenant),
                createDivider(),
                createDataRow("Amount Paid", "\u20b1" + amount),
                createDataRow("Date", date),
                createDataRow("Method", method),
                createDivider(),
                createDataRow("Status", status));
        cardBody.getChildren().addAll(accentBar, content);
        SVGPath tornEdge = new SVGPath();
        tornEdge.setContent(
                "M0 10 L10 0 L20 10 L30 0 L40 10 L50 0 L60 10 L70 0 L80 10 L90 0 " +
                        "L100 10 L110 0 L120 10 L130 0 L140 10 L150 0 L160 10 L170 0 " +
                        "L180 10 L190 0 L200 10 L210 0 L220 10 L220 20 L0 20 Z");
        tornEdge.setStyle("-fx-fill: #eff1f5; -fx-stroke: #dce0e8; -fx-stroke-width: 1;");
        receipt.getChildren().addAll(cardBody, tornEdge);
        return receipt;
    }
    private VBox createDataRow(String labelText, String valueText) {
        VBox row = new VBox(2);
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("receipt-field-label");
        Label val = new Label(valueText);
        val.getStyleClass().add("receipt-field-value");
        row.getChildren().addAll(lbl, val);
        return row;
    }
    private Region createDivider() {
        Region divider = new Region();
        divider.getStyleClass().add("receipt-divider");
        divider.setMaxWidth(Double.MAX_VALUE);
        return divider;
    }
}
