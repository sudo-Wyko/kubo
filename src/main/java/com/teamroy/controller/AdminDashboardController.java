package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Payment;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

public class AdminDashboardController {

    private static final DateTimeFormatter PAYFMT = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a");

    @FXML
    private Label lblTotalRooms;
    @FXML
    private Label lblOccupiedRooms;
    @FXML
    private Label lblAvailableBeds;
    @FXML
    private BarChart<String, Number> revenueChart;
    @FXML
    private Label lblMaintNew;
    @FXML
    private Label lblMaintProgress;
    @FXML
    private ListView<String> expiringLeaseList;
    @FXML
    private ListView<String> recentActivityList;
    @FXML
    private VBox announcementsBox;
    @FXML
    private VBox maintenanceCard;

    private Connection conn;
    private RoomDaoImpl roomDao;
    private LeaseDaoImpl leaseDao;
    private TenantDaoImpl tenantDao;
    private MaintenanceRequestDaoImpl maintenanceDao;
    private PaymentDaoImpl paymentDao;

    private AdminController adminController;

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    private Connection openConnection() throws Exception {
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
        try {
            conn = openConnection();
            roomDao = new RoomDaoImpl(conn);
            leaseDao = new LeaseDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            paymentDao = new PaymentDaoImpl(conn);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        revenueChart.legendVisibleProperty().set(false);
        refreshAll();
    }

    @FXML
    private void handleAddTenant() {
        openTenantDialog(null);
    }

    @FXML
    private void handleCreateLease() {
        openLeaseDialog();
    }

    @FXML
    private void handleAddRoom() {
        openRoomDialog();
    }

    @FXML
    private void handleMaintenanceCardClick() {
        if (adminController != null) {
            adminController.LoadMaintenanceView();
        }
    }

    private void openTenantDialog(Tenant tenant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_tenant_dialog.fxml"));
            Parent root = loader.load();
            AddTenantDialogController c = loader.getController();
            if (tenant == null) {
                c.configureCreate(conn, this::refreshAll);
            } else {
                c.configureEdit(conn, tenant, this::refreshAll);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(tenant == null ? "Add tenant" : "Edit tenant");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openLeaseDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_lease_dialog.fxml"));
            Parent root = loader.load();
            AddLeaseDialogController c = loader.getController();
            c.configureCreate(conn, this::refreshAll);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create lease");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openRoomDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_room_dialog.fxml"));
            Parent root = loader.load();
            AddRoomDialogController c = loader.getController();
            c.configureCreate(conn, this::refreshAll);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add room");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void refreshAll() {
        if (paymentDao == null) {
            return;
        }

        refreshOccupancy();
        refreshRevenueChart();
        refreshMaintenanceCounts();
        refreshExpiring();
        refreshRecentPayments();
        loadAnnouncements();
    }

    private void refreshOccupancy() {
        List<Room> rooms = roomDao.GetAll();
        int totalRooms = rooms.size();
        long occupiedRooms = rooms.stream().filter(r -> r.GetCurrentOccupancy() > 0).count();
        int availableBeds = rooms.stream().mapToInt(r -> Math.max(r.GetCapacity() - r.GetCurrentOccupancy(), 0)).sum();

        lblTotalRooms.setText("Total rooms: " + totalRooms);
        lblOccupiedRooms.setText("Occupied rooms: " + occupiedRooms);
        lblAvailableBeds.setText("Available beds: " + availableBeds);
    }

    private void refreshRevenueChart() {
        revenueChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Verified Payments");

        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime end = start.plusMonths(1).minusSeconds(1);
            List<Payment> payments = paymentDao.GetByDateRange(start, end);
            double total = payments.stream()
                    .filter(p -> "VERIFIED".equalsIgnoreCase(p.GetStatus()))
                    .mapToDouble(Payment::GetAmountPaid)
                    .sum();
            String label = start.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH)
                    + " " + start.getYear();
            series.getData().add(new XYChart.Data<>(label, total));
        }

        revenueChart.getData().add(series);
    }

    private void refreshMaintenanceCounts() {
        List<MaintenanceRequest> allNew = maintenanceDao.GetByStatus("NEW");
        List<MaintenanceRequest> wip = maintenanceDao.GetByStatus("IN-PROGRESS");
        lblMaintNew.setText("NEW: " + allNew.size());
        lblMaintProgress.setText("IN-PROGRESS: " + wip.size());
    }

    private void refreshExpiring() {
        List<Lease> leases = leaseDao.GetExpiringSoon(LocalDate.now().plusDays(30));
        List<String> lines = leases.stream().map(lease -> {
            Tenant tenant = tenantDao.GetByID(lease.GetTenantID());
            Room room = roomDao.GetByID(lease.GetRoomID());
            String tenantName = tenant == null ? ("Tenant#" + lease.GetTenantID())
                    : (tenant.GetFirstName() + " " + tenant.GetLastName());
            String roomLabel = room == null ? ("Room#" + lease.GetRoomID()) : room.GetRoomNumber();
            return tenantName + " • Room " + roomLabel + " • ends "
                    + lease.GetEndDate() + " • " + CurrencyUtil.format(lease.GetMonthlyRent());
        }).collect(Collectors.toList());

        expiringLeaseList.setItems(FXCollections.observableArrayList(lines));
    }

    private void refreshRecentPayments() {
        List<Payment> payments = paymentDao.GetAll().stream()
                .sorted(Comparator.comparing(Payment::GetPaymentDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<String> lines = payments.stream().map(payment -> {
            Tenant t = tenantDao.GetByID(payment.GetTenantID());
            String name = t == null ? ("#" + payment.GetTenantID()) : (t.GetFirstName() + " " + t.GetLastName());
            String when = payment.GetPaymentDate() == null ? "—" : PAYFMT.format(payment.GetPaymentDate());
            String amount = CurrencyUtil.format(payment.GetAmountPaid());
            return when + " • " + name + " • " + amount + " • " + payment.GetStatus();
        }).collect(Collectors.toList());

        recentActivityList.setItems(FXCollections.observableArrayList(lines));
    }

    private void loadAnnouncements() {
        announcementsBox.getChildren().clear();
        InputStream stream = getClass().getResourceAsStream("/com/teamroy/announcements.txt");
        if (stream == null) {
            Label missing = new Label("No announcements.txt found.");
            missing.setWrapText(true);
            missing.setStyle("-fx-text-fill: #fca5a5;");
            announcementsBox.getChildren().add(missing);
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                Label lbl = new Label("• " + trimmed);
                lbl.setWrapText(true);
                lbl.setStyle("-fx-text-fill: #e2e8f0;");
                announcementsBox.getChildren().add(lbl);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Label lbl = new Label("Could not load announcements.");
            lbl.setWrapText(true);
            lbl.setStyle("-fx-text-fill: #fca5a5;");
            announcementsBox.getChildren().add(lbl);
        }
    }
}
