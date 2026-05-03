package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

public class TenantPaymentsController {

    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML
    private Label balanceSummaryLabel;
    @FXML
    private TableView<Payment> paymentsTable;
    @FXML
    private TableColumn<Payment, String> colPayDate;
    @FXML
    private TableColumn<Payment, String> colPayAmount;
    @FXML
    private TableColumn<Payment, String> colPayMethod;
    @FXML
    private TableColumn<Payment, Label> colPayStatus;

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

    private Label buildPaymentStatusBadge(String status) {
        Label label = new Label(status == null ? "" : status);
        String bg = "#334155";
        if ("PENDING".equalsIgnoreCase(status)) {
            bg = "#ca8a04";
        } else if ("VERIFIED".equalsIgnoreCase(status)) {
            bg = "#15803d";
        } else if ("FAILED".equalsIgnoreCase(status)) {
            bg = "#b91c1c";
        }
        label.setStyle("-fx-background-color: " + bg
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 10;");
        return label;
    }

    private void configureTable() {
        colPayDate.setCellValueFactory(cd -> {
            if (cd.getValue().GetPaymentDate() == null) {
                return new ReadOnlyObjectWrapper<>("—");
            }
            return new ReadOnlyObjectWrapper<>(DISPLAY.format(cd.getValue().GetPaymentDate()));
        });
        colPayAmount.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(CurrencyUtil.format(cd.getValue().GetAmountPaid())));
        colPayMethod.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().GetPaymentMethod()));

        colPayStatus.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(buildPaymentStatusBadge(cd.getValue().GetStatus())));
        colPayStatus.setCellFactory(column -> new TableCell<Payment, Label>() {
            @Override
            protected void updateItem(Label item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(item);
                }
            }
        });
    }

    @FXML
    private void initialize() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            balanceSummaryLabel.setText("Current Balance: unavailable");
            paymentsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        configureTable();
        reload(tenant.GetTenantID());
    }

    private void reload(int tenantId) {
        try (Connection conn = getConnection()) {
            TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
            Tenant refreshed = tenantDao.GetByID(tenantId);
            double balance = refreshed != null ? refreshed.GetTotalBalance() : SessionManager.getCurrentTenant().GetTotalBalance();
            balanceSummaryLabel.setText("Current Balance: " + CurrencyUtil.format(balance));

            PaymentDaoImpl paymentDao = new PaymentDaoImpl(conn);
            List<Payment> payments = paymentDao.GetByTenantID(tenantId);
            paymentsTable.setItems(FXCollections.observableArrayList(payments));
            paymentsTable.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            balanceSummaryLabel.setText("Current Balance: error");
        }
    }

    @FXML
    private void handleSubmitPayment() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            return;
        }

        try {
            Connection bootstrap = getConnection();
            TenantDaoImpl tenantDao = new TenantDaoImpl(bootstrap);
            Tenant refreshed = tenantDao.GetByID(tenant.GetTenantID());
            Tenant effective = refreshed != null ? refreshed : tenant;
            int tenantId = effective.GetTenantID();
            bootstrap.close();

            Connection conn = getConnection();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_payment_dialog.fxml"));
            Parent root = loader.load();
            AddPaymentDialogController controller = loader.getController();
            controller.configureTenantSubmit(conn, effective, 0.0, () -> reload(tenantId));

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Submit payment");
            stage.setScene(new Scene(root));
            stage.setOnHidden(e -> {
                try {
                    conn.close();
                } catch (Exception ignored) {
                    // ignore
                }
            });
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
