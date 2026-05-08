package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.ImportExportService;
import com.teamroy.service.PaymentService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminPaymentController {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    @FXML
    private ComboBox<Tenant> tenantFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private DatePicker monthFilterPicker;
    @FXML
    private TableView<Payment> paymentTable;
    @FXML
    private TableColumn<Payment, Integer> colPaymentId;
    @FXML
    private TableColumn<Payment, String> colTenant;
    @FXML
    private TableColumn<Payment, String> colAmount;
    @FXML
    private TableColumn<Payment, String> colDate;
    @FXML
    private TableColumn<Payment, String> colMethod;
    @FXML
    private TableColumn<Payment, Label> colStatus;
    @FXML
    private TableColumn<Payment, Void> colActions;
    @FXML
    private Label verifiedTotalLabel;
    @FXML
    private Label pendingCardLabel;
    @FXML
    private Label verifiedCardLabel;
    @FXML
    private Label failedCardLabel;

    private Connection conn;
    private PaymentDaoImpl paymentDao;
    private TenantDaoImpl tenantDao;
    private PaymentService paymentService;
    private final ImportExportService importExportService = new ImportExportService();
    private final Map<Integer, String> tenantNameCache = new HashMap<>();

    private Tenant createAllTenantsOption() {
        Tenant sentinel = new Tenant();
        sentinel.SetTenantID(-1);
        sentinel.SetFirstName("All");
        sentinel.SetLastName("Tenants");
        return sentinel;
    }

    @FXML
    private void initialize() {
        try {
            conn = ConnectionManager.getConnection();
            paymentDao = new PaymentDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            paymentService = new PaymentService(conn);
        } catch (Exception ex) {
            System.err.println("Failed to initialize payments view: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        tenantFilterCombo.getItems().add(createAllTenantsOption());
        tenantFilterCombo.getItems().addAll(tenantDao.GetAllActive());
        tenantFilterCombo.getSelectionModel().selectFirst();
        bindTenantFilterCells();

        statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "PENDING", "VERIFIED", "FAILED"));
        statusFilterCombo.getSelectionModel().selectFirst();

        tenantFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshTable());
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> refreshTable());
        monthFilterPicker.valueProperty().addListener((obs, o, n) -> refreshTable());

        configureColumns();
        refreshTenantNames();
        refreshTable();
    }

    private void bindTenantFilterCells() {
        tenantFilterCombo.setCellFactory(lv -> new ListCell<Tenant>() {
            @Override
            protected void updateItem(Tenant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetTenantID() < 0) {
                    setText("All Tenants");
                } else {
                    setText(item.GetFirstName() + " " + item.GetLastName());
                }
            }
        });
        tenantFilterCombo.setButtonCell(new ListCell<Tenant>() {
            @Override
            protected void updateItem(Tenant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetTenantID() < 0) {
                    setText("All Tenants");
                } else {
                    setText(item.GetFirstName() + " " + item.GetLastName());
                }
            }
        });
    }

    private void configureColumns() {
        colPaymentId.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().GetPaymentID()));

        colTenant.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                tenantNameCache.getOrDefault(cd.getValue().GetTenantID(), "Tenant #" + cd.getValue().GetTenantID())));

        colAmount.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(CurrencyUtil.format(cd.getValue().GetAmountPaid())));

        colDate.setCellValueFactory(cd -> {
            LocalDateTime dt = cd.getValue().GetPaymentDate();
            String formatted = dt == null ? "" : DATE_TIME_FORMAT.format(dt);
            return new ReadOnlyObjectWrapper<>(formatted);
        });

        colMethod.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().GetPaymentMethod()));

        colStatus.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(createStatusBadge(cd.getValue().GetStatus())));
        colStatus.setCellFactory(column -> new TableCell<Payment, Label>() {
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

        colActions.setCellFactory(col -> new TableCell<Payment, Void>() {
            private final ComboBox<String> combo = new ComboBox<>(
                    FXCollections.observableArrayList("PENDING", "VERIFIED", "FAILED"));
            private boolean suppress;

            {
                combo.valueProperty().addListener((obs, oldVal, newVal) -> {
                    if (suppress || oldVal == null || getTableRow() == null || getTableRow().getItem() == null
                            || newVal == null) {
                        return;
                    }
                    Payment rowPayment = getTableRow().getItem();
                    if (oldVal.equals(newVal)) {
                        return;
                    }
                    handleUpdateStatus(rowPayment, newVal);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Payment payment = getTableRow().getItem();
                suppress = true;
                combo.setValue(payment.GetStatus());
                suppress = false;
                setGraphic(combo);
            }
        });
    }

    private Label createStatusBadge(String status) {
        Label label = new Label(status == null ? "" : status);
        label.setStyle(statusBadgeStyle(status));
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private String statusBadgeStyle(String status) {
        String bg = "#334155";
        if ("PENDING".equalsIgnoreCase(status)) {
            bg = "#ca8a04";
        } else if ("VERIFIED".equalsIgnoreCase(status)) {
            bg = "#15803d";
        } else if ("FAILED".equalsIgnoreCase(status)) {
            bg = "#b91c1c";
        }
        return "-fx-background-color: " + bg + "; -fx-text-fill: white; -fx-padding: 4 10 4 10; -fx-background-radius: 12;";
    }

    private void refreshTenantNames() {
        tenantNameCache.clear();
        for (Tenant tenant : tenantDao.GetAllActive()) {
            tenantNameCache.put(tenant.GetTenantID(),
                    tenant.GetFirstName() + " " + tenant.GetLastName());
        }
        for (Tenant tenant : tenantDao.GetAll()) {
            tenantNameCache.putIfAbsent(tenant.GetTenantID(),
                    tenant.GetFirstName() + " " + tenant.GetLastName());
        }
    }

    /** Current filter view of payments (matches the table). */
    private List<Payment> getFilteredPayments() {
        return applyFilters(new ArrayList<>(paymentDao.GetAll()));
    }

    private List<Payment> applyFilters(List<Payment> source) {
        List<Payment> list = new ArrayList<>(source);

        Tenant tenantSelection = tenantFilterCombo.getSelectionModel().getSelectedItem();
        if (tenantSelection != null && tenantSelection.GetTenantID() >= 0) {
            final int tenantId = tenantSelection.GetTenantID();
            list = list.stream().filter(p -> p.GetTenantID() == tenantId).collect(Collectors.toList());
        }

        String statusSelection = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (statusSelection != null && !"ALL".equals(statusSelection)) {
            list = list.stream().filter(p -> statusSelection.equalsIgnoreCase(p.GetStatus()))
                    .collect(Collectors.toList());
        }

        LocalDate monthSelection = monthFilterPicker.getValue();
        if (monthSelection != null) {
            LocalDate start = monthSelection.withDayOfMonth(1);
            LocalDate end = monthSelection.withDayOfMonth(monthSelection.lengthOfMonth());
            LocalDateTime startDt = start.atStartOfDay();
            LocalDateTime endDt = end.atTime(23, 59, 59);
            list = list.stream().filter(p -> {
                LocalDateTime dt = p.GetPaymentDate();
                return dt != null && !dt.isBefore(startDt) && !dt.isAfter(endDt);
            }).collect(Collectors.toList());
        }

        return list;
    }

    private void refreshTable() {
        if (paymentDao == null) {
            return;
        }

        refreshTenantNames();

        List<Payment> payments = getFilteredPayments();

        ObservableList<Payment> rows = FXCollections.observableArrayList(payments);
        paymentTable.setItems(rows);

        double pendingSum = payments.stream()
                .filter(p -> "PENDING".equalsIgnoreCase(p.GetStatus()))
                .mapToDouble(Payment::GetAmountPaid)
                .sum();
        double verifiedSum = payments.stream()
                .filter(p -> "VERIFIED".equalsIgnoreCase(p.GetStatus()))
                .mapToDouble(Payment::GetAmountPaid)
                .sum();
        double failedSum = payments.stream()
                .filter(p -> "FAILED".equalsIgnoreCase(p.GetStatus()))
                .mapToDouble(Payment::GetAmountPaid)
                .sum();

        pendingCardLabel.setText(CurrencyUtil.format(pendingSum));
        verifiedCardLabel.setText(CurrencyUtil.format(verifiedSum));
        failedCardLabel.setText(CurrencyUtil.format(failedSum));
        verifiedTotalLabel.setText(CurrencyUtil.format(verifiedSum));

        paymentTable.refresh();
    }

    void handleUpdateStatus(Payment payment, String newStatus) {
        if (payment == null || newStatus == null || newStatus.equals(payment.GetStatus())) {
            return;
        }

        if ("VERIFIED".equalsIgnoreCase(newStatus) && !"VERIFIED".equalsIgnoreCase(payment.GetStatus())) {
            paymentService.verifyPayment(payment.GetPaymentID(), payment.GetTenantID(),
                    payment.GetAmountPaid());
        } else {
            paymentDao.UpdateStatus(payment.GetPaymentID(), newStatus);
        }

        refreshTable();
    }

    @FXML
    private void handleExportPaymentsCsv() {
        if (paymentDao == null || paymentTable == null || paymentTable.getScene() == null) {
            return;
        }
        Stage stage = (Stage) paymentTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export payments");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File dest = chooser.showSaveDialog(stage);
        if (dest == null) {
            return;
        }
        try {
            importExportService.exportPaymentsToCSV(getFilteredPayments(), dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Export failed");
            a.setHeaderText(null);
            a.setContentText(ex.getMessage() == null ? "Could not write file." : ex.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void handleExportPaymentsJson() {
        if (paymentDao == null || paymentTable == null || paymentTable.getScene() == null) {
            return;
        }
        Stage stage = (Stage) paymentTable.getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export payments");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File dest = chooser.showSaveDialog(stage);
        if (dest == null) {
            return;
        }
        try {
            importExportService.exportPaymentsToJSON(getFilteredPayments(), dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            Alert a = new Alert(Alert.AlertType.ERROR);
            a.setTitle("Export failed");
            a.setHeaderText(null);
            a.setContentText(ex.getMessage() == null ? "Could not write file." : ex.getMessage());
            a.showAndWait();
        }
    }

    @FXML
    private void handleAddPayment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_payment_dialog.fxml"));
            Parent root = loader.load();
            AddPaymentDialogController controller = loader.getController();
            controller.configureAdmin(conn, this::refreshTable);
            controller.setTenants(tenantDao.GetAllActive());

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add payment");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
