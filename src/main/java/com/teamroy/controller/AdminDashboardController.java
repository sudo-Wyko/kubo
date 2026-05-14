package com.teamroy.controller;
import com.teamroy.CurrencyUtil;
import com.teamroy.ConnectionManager;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
public class AdminDashboardController {
    private static final DateTimeFormatter PAYFMT = DateTimeFormatter.ofPattern("MMM d yyyy h:mm a");
    private static final DateTimeFormatter ANNOUNCEMENT_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");
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
            System.err.println("Failed to initialize dashboard: " + ex.getMessage());
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
            adminController.loadMaintenanceView();
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
            Tenant t = tenantDao.GetByID(payment.GetTenantID());
            String name = t == null ? ("#" + payment.GetTenantID()) : (t.GetFirstName() + " " + t.GetLastName());
            String when = payment.GetPaymentDate() == null ? "\u2014" : PAYFMT.format(payment.GetPaymentDate());
            String amount = CurrencyUtil.format(payment.GetAmountPaid());
            return when + " \u2022 " + name + " \u2022 " + amount + " \u2022 " + payment.GetStatus();
        }).collect(Collectors.toList());
        recentActivityList.setItems(FXCollections.observableArrayList(lines));
    }
    private void loadAnnouncements() {
        announcementsBox.getChildren().clear();
        if (announcementDao == null) {
            Label err = new Label("Announcements unavailable.");
            err.setWrapText(true);
            err.getStyleClass().add("error-label");
            announcementsBox.getChildren().add(err);
            return;
        }
        Button btnNew = new Button("+ New Announcement");
        btnNew.getStyleClass().add("primary-button");
        btnNew.setMaxWidth(Double.MAX_VALUE);
        btnNew.setOnAction(e -> openNewAnnouncementDialog());
        announcementsBox.getChildren().add(btnNew);
        List<Announcement> list = announcementDao.GetActive();
        if (list.isEmpty()) {
            Label empty = new Label("No announcements yet.");
            empty.getStyleClass().add("card-subtitle");
            announcementsBox.getChildren().add(empty);
            return;
        }
        for (Announcement a : list) {
            announcementsBox.getChildren().add(buildAnnouncementAdminRow(a));
        }
    }
    private VBox buildAnnouncementAdminRow(Announcement a) {
        VBox card = new VBox(6);
        HBox top = new HBox(8);
        top.setAlignment(Pos.CENTER_LEFT);
        Label lblTitle = new Label(a.GetTitle() != null ? a.GetTitle() : "");
        lblTitle.getStyleClass().add("card-title");
        lblTitle.setWrapText(true);
        HBox.setHgrow(lblTitle, Priority.ALWAYS);
        Button btnDel = new Button("Delete");
        btnDel.getStyleClass().add("secondary-button");
        final int announcementId = a.GetAnnouncementID();
        btnDel.setOnAction(ev -> {
            announcementDao.SoftDelete(announcementId);
            loadAnnouncements();
        });
        top.getChildren().addAll(lblTitle, btnDel);
        String when = a.GetDatePosted() != null ? ANNOUNCEMENT_DATE.format(a.GetDatePosted()) : "";
        Label lblWhen = new Label(when);
        lblWhen.getStyleClass().add("card-subtitle");
        Label lblMsg = new Label(a.GetMessage() != null ? a.GetMessage() : "");
        lblMsg.setWrapText(true);
        lblMsg.getStyleClass().add("row-description");
        card.getChildren().addAll(top, lblWhen, lblMsg);
        return card;
    }
    private void openNewAnnouncementDialog() {
        Window owner = announcementsBox.getScene() != null ? announcementsBox.getScene().getWindow() : null;
        Stage dialog = new Stage();
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New announcement");
        Label lblTitle = new Label("Title");
        lblTitle.getStyleClass().add("card-subtitle");
        TextField titleField = new TextField();
        titleField.setPromptText("Title");
        Label lblMsg = new Label("Message");
        lblMsg.getStyleClass().add("card-subtitle");
        TextArea messageField = new TextArea();
        messageField.setPromptText("Message");
        messageField.setWrapText(true);
        messageField.setPrefRowCount(5);
        Button btnSave = new Button("Save");
        btnSave.getStyleClass().add("primary-button");
        Button btnCancel = new Button("Cancel");
        btnCancel.getStyleClass().add("secondary-button");
        btnCancel.setOnAction(e -> dialog.close());
        btnSave.setOnAction(e -> {
            String t = titleField.getText() != null ? titleField.getText().trim() : "";
            String m = messageField.getText() != null ? messageField.getText().trim() : "";
            if (t.isEmpty() || m.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation");
                alert.setHeaderText(null);
                alert.setContentText("Title and message cannot be blank.");
                if (owner != null) {
                    alert.initOwner(owner);
                }
                alert.showAndWait();
                return;
            }
            announcementDao.Create(new Announcement(t, m, null));
            dialog.close();
            loadAnnouncements();
        });
        HBox btnRow = new HBox(10, btnSave, btnCancel);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        VBox root = new VBox(10, lblTitle, titleField, lblMsg, messageField, btnRow);
        root.setPadding(new Insets(16));
        Scene scene = new Scene(root, 400, 300);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
