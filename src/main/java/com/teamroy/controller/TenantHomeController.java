package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class TenantHomeController {

    private static final DateTimeFormatter PAYMENT_LINE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a");

    @FXML
    private Label greetingLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Label dueDateLabel;
    @FXML
    private Button payNowButton;
    @FXML
    private TableView<MaintenanceRequest> maintenanceTable;
    @FXML
    private TableColumn<MaintenanceRequest, String> colMaintDescription;
    @FXML
    private TableColumn<MaintenanceRequest, String> colMaintDate;
    @FXML
    private TableColumn<MaintenanceRequest, Label> colMaintStatus;
    @FXML
    private ListView<String> activityList;
    @FXML
    private VBox announcementsBox;

    private TenantController parentController;

    public void setParentController(TenantController parentController) {
        this.parentController = parentController;
    }

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

    private Label buildStatusBadge(String status) {
        Label label = new Label(status == null ? "" : status);
        String bg = "#334155";
        if ("PENDING".equalsIgnoreCase(status)) {
            bg = "#ca8a04";
        } else if ("VERIFIED".equalsIgnoreCase(status)) {
            bg = "#15803d";
        } else if ("FAILED".equalsIgnoreCase(status)) {
            bg = "#b91c1c";
        } else if ("NEW".equalsIgnoreCase(status)) {
            bg = "#2563eb";
        } else if ("IN-PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
            bg = "#ca8a04";
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            bg = "#15803d";
        }
        label.setStyle("-fx-background-color: " + bg
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 10;");
        return label;
    }

    @FXML
    private void initialize() {
        loadAnnouncementsFromFile();

        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            greetingLabel.setText("Welcome!");
            balanceLabel.setText("Unavailable");
            dueDateLabel.setText("Tenant profile not linked to this account.");
            payNowButton.setDisable(true);
            return;
        }

        try (Connection conn = getConnection()) {
            TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
            Tenant refreshed = tenantDao.GetByID(tenant.GetTenantID());
            if (refreshed != null) {
                tenant = refreshed;
            }

            greetingLabel.setText("Welcome, " + tenant.GetFirstName() + "!");

            double balance = tenant.GetTotalBalance();
            balanceLabel.setText(CurrencyUtil.format(balance));
            balanceLabel.setStyle(balance > 0
                    ? "-fx-text-fill: #f87171; -fx-font-size: 36px; -fx-font-weight: bold;"
                    : "-fx-text-fill: #34d399; -fx-font-size: 36px; -fx-font-weight: bold;");

            LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
            List<Lease> leases = leaseDao.GetByTenantId(tenant.GetTenantID());
            Lease activeLease =
                    leases.stream()
                            .filter(l -> l != null && "ACTIVE".equalsIgnoreCase(l.GetStatus()))
                            .findFirst()
                            .orElse(null);

            if (activeLease == null) {
                dueDateLabel.setText("No active lease — rent timing unavailable.");
            } else {
                dueDateLabel.setText("Rent due by " + activeLease.GetStartDate().plusMonths(1));
            }

            MaintenanceRequestDaoImpl maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            List<MaintenanceRequest> requests = maintenanceDao.GetByTenantID(tenant.GetTenantID());

            configureMaintenanceTable();
            maintenanceTable.setItems(FXCollections.observableArrayList(requests));

            PaymentDaoImpl paymentDao = new PaymentDaoImpl(conn);
            List<Payment> payments = paymentDao.GetByTenantID(tenant.GetTenantID());
            List<String> activity = payments.stream()
                    .sorted(Comparator.comparing(Payment::GetPaymentDate,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .map(p -> {
                        String when = p.GetPaymentDate() == null ? "—"
                                : PAYMENT_LINE_FORMAT.format(p.GetPaymentDate());
                        return when + " • " + CurrencyUtil.format(p.GetAmountPaid()) + " • "
                                + p.GetPaymentMethod() + " • " + p.GetStatus();
                    })
                    .collect(Collectors.toList());
            activityList.setItems(FXCollections.observableArrayList(activity));

        } catch (Exception ex) {
            ex.printStackTrace();
            greetingLabel.setText("Welcome!");
            balanceLabel.setText("Error loading profile");
            dueDateLabel.setText("Could not reach the database.");
            payNowButton.setDisable(true);
        }
    }

    private void loadAnnouncementsFromFile() {
        if (announcementsBox == null) {
            return;
        }
        announcementsBox.getChildren().clear();
        InputStream is = getClass().getResourceAsStream("/com/teamroy/announcements.txt");
        if (is == null) {
            announcementsBox.getChildren().add(new Label("(No announcements file.)"));
            return;
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Label lbl = new Label(trimmed);
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: #cbd5f5;");
                announcementsBox.getChildren().add(lbl);
            }
        } catch (IOException ex) {
            announcementsBox.getChildren().add(new Label("Could not load announcements."));
        }
    }

    private void configureMaintenanceTable() {
        colMaintDescription.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().GetReportDescription()));

        colMaintDate.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                cd.getValue().GetReportedDate() == null ? ""
                        : cd.getValue().GetReportedDate().toLocalDate().toString()));

        colMaintStatus.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(buildStatusBadge(cd.getValue().GetStatus())));
        colMaintStatus.setCellFactory(column -> new TableCell<MaintenanceRequest, Label>() {
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
    private void handlePayNow() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            return;
        }

        try {
            Connection bootstrap = getConnection();
            TenantDaoImpl tenantDao = new TenantDaoImpl(bootstrap);
            Tenant refreshed = tenantDao.GetByID(tenant.GetTenantID());
            double preset = refreshed != null ? refreshed.GetTotalBalance() : tenant.GetTotalBalance();
            Tenant effective = refreshed != null ? refreshed : tenant;
            bootstrap.close();

            openTenantPaymentDialog(effective, preset);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openTenantPaymentDialog(Tenant tenant, double presetAmount) throws IOException {
        Connection conn;
        try {
            conn = getConnection();
        } catch (Exception ex) {
            throw new IOException(ex);
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_payment_dialog.fxml"));
        Parent root = loader.load();
        AddPaymentDialogController controller = loader.getController();
        controller.configureTenantSubmit(conn, tenant, presetAmount, this::initialize);

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
    }

    @FXML
    private void handleQuickPayRent() {
        if (parentController != null) {
            parentController.LoadPaymentsView();
        }
    }

    @FXML
    private void handleQuickMaintenance() {
        if (parentController != null) {
            parentController.LoadMaintenanceView();
        }
    }

    @FXML
    private void handleQuickLease() {
        if (parentController != null) {
            parentController.LoadLeaseView();
        }
    }
}
