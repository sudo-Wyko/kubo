package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.DaoException;
import com.teamroy.model.dao.AnnouncementDaoImpl;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.PaymentDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Announcement;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AdminDashboardController {
    private static final Logger LOGGER = Logger.getLogger(AdminDashboardController.class.getName());
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
    private AnnouncementDaoImpl announcementDao;
    private AdminController adminController;

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    @FXML
    private void initialize() {
        try {
            conn = ConnectionManager.getConnection();
            roomDao = new RoomDaoImpl(conn);
            leaseDao = new LeaseDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            paymentDao = new PaymentDaoImpl(conn);
            announcementDao = new AnnouncementDaoImpl(conn);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize dashboard", ex);
            showError("Dashboard unavailable", ex.getMessage());
            return;
        }
        revenueChart.legendVisibleProperty().set(false);
        refreshAll();
    }

    @FXML
    private void handleAddCharge() {
        runQuickAction("Add Charge", () -> {
            try {
                openChargeDialog();
            } catch (Exception ex) {
                throw wrapActionFailure(ex);
            }
        });
    }

    @FXML
    private void handleAddTenant() {
        runQuickAction("Add Tenant", () -> {
            try {
                openTenantDialog(null);
            } catch (Exception ex) {
                throw wrapActionFailure(ex);
            }
        });
    }

    @FXML
    private void handleCreateLease() {
        runQuickAction("Create Lease", () -> {
            try {
                openLeaseDialog();
            } catch (Exception ex) {
                throw wrapActionFailure(ex);
            }
        });
    }

    @FXML
    private void handleCreateAnnouncement() {
        runQuickAction("Create Announcement", () -> {
            try {
                openAnnouncementDialog();
            } catch (Exception ex) {
                throw wrapActionFailure(ex);
            }
        });
    }

    @FXML
    private void handleMaintenanceCardClick() {
        if (adminController != null) {
            adminController.loadMaintenanceView();
        }
    }

    private void runQuickAction(String actionName, Runnable action) {
        try {
            action.run();
        } catch (DaoException ex) {
            LOGGER.log(Level.WARNING, actionName + " failed: " + ex.getMessage());
            showError(actionName + " failed", ex.getMessage());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, actionName + " failed", ex);
            showError(actionName + " failed", ex.getMessage() == null ? "Unexpected error." : ex.getMessage());
        }
    }

    private void openTenantDialog(Tenant tenant) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_tenant_dialog.fxml"));
        Parent root = loader.load();
        AddTenantDialogController controller = loader.getController();
        if (tenant == null) {
            controller.configureCreate(conn, this::refreshAll);
        } else {
            controller.configureEdit(conn, tenant, this::refreshAll);
        }
        showModal("Add tenant", root);
    }

    private void openLeaseDialog() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_lease_dialog.fxml"));
        Parent root = loader.load();
        AddLeaseDialogController controller = loader.getController();
        controller.configureCreate(conn, this::refreshAll);
        showModal("Create lease", root);
    }

    private void openAnnouncementDialog() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_announcement_dialog.fxml"));
        Parent root = loader.load();
        AddAnnouncementDialogController controller = loader.getController();
        controller.configure(conn, this::refreshAll);
        showModal("Create Announcement", root);
    }

    private void openChargeDialog() throws Exception {
        List<Lease> leases = leaseDao.GetAll().stream()
                .filter(l -> !"TERMINATED".equalsIgnoreCase(l.GetStatus()))
                .collect(Collectors.toList());
        if (leases.isEmpty()) {
            showError("Issue charge", "No eligible leases found. Create a lease first.");
            return;
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_charge_dialog.fxml"));
        Parent root = loader.load();
        AddChargeDialogController controller = loader.getController();
        controller.configure(conn, this::refreshAll);
        controller.setLeases(leases);
        showModal("Add Charge", root);
    }

    private void showModal(String title, Parent root) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(title);
        Scene scene = new Scene(root);
        DialogUiHelper.applyStyles(scene);
        stage.setScene(scene);
        stage.showAndWait();
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
            return tenantName + " \u2022 Room " + roomLabel + " \u2022 ends "
                    + lease.GetEndDate() + " \u2022 " + CurrencyUtil.format(lease.GetMonthlyRent());
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
            Lease lease = leaseDao.GetByID(payment.GetLeaseID());
            Tenant t = lease == null ? null : tenantDao.GetByID(lease.GetTenantID());
            String name = t == null ? ("Lease #" + payment.GetLeaseID()) : (t.GetFirstName() + " " + t.GetLastName());
            String when = payment.GetPaymentDate() == null ? "\u2014" : PAYFMT.format(payment.GetPaymentDate());
            String amount = CurrencyUtil.format(payment.GetAmountPaid());
            return when + " \u2022 " + name + " \u2022 " + amount + " \u2022 " + payment.GetStatus();
        }).collect(Collectors.toList());
        recentActivityList.setItems(FXCollections.observableArrayList(lines));
    }

    private void loadAnnouncements() {
        announcementsBox.getChildren().clear();
        if (announcementDao == null) {
            return;
        }
        List<Announcement> announcements = announcementDao.GetRecentAnnouncements(5);
        if (announcements.isEmpty()) {
            Label empty = new Label("No announcements yet.");
            empty.setWrapText(true);
            empty.getStyleClass().add("card-subtitle");
            announcementsBox.getChildren().add(empty);
            return;
        }
        for (Announcement announcement : announcements) {
            Label title = new Label(announcement.GetTitle());
            title.getStyleClass().add("announcement-title");
            Label message = new Label(announcement.GetMessage());
            message.setWrapText(true);
            message.getStyleClass().add("row-description");
            announcementsBox.getChildren().addAll(title, message);
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message == null ? "An error occurred." : message);
        alert.showAndWait();
    }

    private static RuntimeException wrapActionFailure(Exception ex) {
        if (ex instanceof RuntimeException) {
            return (RuntimeException) ex;
        }
        return new RuntimeException(ex);
    }
}
