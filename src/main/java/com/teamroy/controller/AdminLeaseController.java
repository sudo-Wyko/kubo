package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Tenant;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AdminLeaseController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ScrollPane leasesScrollPane;
    @FXML
    private FlowPane leasesFlowPane;

    private Connection conn;
    private LeaseDaoImpl leaseDao;
    private TenantDaoImpl tenantDao;
    private RoomDaoImpl roomDao;

    private List<Lease> cachedLeases = List.of();
    private Map<Integer, String> tenantNameById = Map.of();

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    @FXML
    private void initialize() {
        try {
            conn = ConnectionManager.getConnection();
            leaseDao = new LeaseDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            roomDao = new RoomDaoImpl(conn);
        } catch (Exception ex) {
            System.err.println("Failed to initialize leases view: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        leasesScrollPane.setFitToWidth(true);

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "All", "ACTIVE", "EXPIRED", "TERMINATED"));
        statusFilterCombo.getSelectionModel().selectFirst();
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> rebuildFiltered());

        searchDebounce.setOnFinished(e -> rebuildFiltered());
        searchField.textProperty().addListener((o, ov, nv) -> searchDebounce.playFromStart());

        reloadFromDatabase();
    }

    @FXML
    private void handleAddLease() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_lease_dialog.fxml"));
            Parent root = loader.load();
            AddLeaseDialogController c = loader.getController();
            c.configureCreate(conn, this::reloadFromDatabase);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create lease");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void reloadFromDatabase() {
        cachedLeases = leaseDao.GetAll();
        tenantNameById = tenantDao.GetAll().stream()
                .collect(Collectors.toMap(Tenant::GetTenantID, t -> combineName(t), (a, b) -> a));

        Map<Integer, String> roomNumberByRoomId =
                roomDao.GetAll().stream().collect(Collectors.toMap(
                        r -> r.GetRoomID(), r -> r.GetRoomNumber(), (a, b) -> a));
        rebuildFiltered(roomNumberByRoomId);
    }

    private Set<Integer> expiringLeaseIds() {
        Set<Integer> set = new HashSet<>();
        for (Lease l : leaseDao.GetExpiringSoon(LocalDate.now().plusDays(30))) {
            set.add(l.GetLeaseID());
        }
        return set;
    }

    private void rebuildFiltered() {
        Map<Integer, String> roomNumberByRoomId = roomDao.GetAll().stream().collect(Collectors.toMap(
                r -> r.GetRoomID(), r -> r.GetRoomNumber(), (a, b) -> a));
        rebuildFiltered(roomNumberByRoomId);
    }

    private void rebuildFiltered(Map<Integer, String> roomNumberByRoomId) {
        leasesFlowPane.getChildren().clear();

        String qRaw = searchField.getText().trim().toLowerCase();
        String statusPick = statusFilterCombo.getSelectionModel().getSelectedItem();

        Set<Integer> expiring = expiringLeaseIds();

        List<Lease> rows =
                cachedLeases.stream().filter(lease -> filterLease(lease, statusPick, qRaw, roomNumberByRoomId))
                        .sorted((a, b) -> Integer.compare(a.GetLeaseID(), b.GetLeaseID()))
                        .collect(Collectors.toList());

        for (Lease lease : rows) {
            String tenantLabel = tenantLabel(lease.GetTenantID());
            String roomLabel = roomNumberByRoomId.getOrDefault(lease.GetRoomID(),
                    Integer.toString(lease.GetRoomID()));
            boolean amber = expiring.contains(lease.GetLeaseID())
                    && "ACTIVE".equalsIgnoreCase(lease.GetStatus());

            leasesFlowPane.getChildren().add(buildLeaseCard(lease, tenantLabel, roomLabel, amber));
        }
    }

    private boolean filterLease(
            Lease lease, String statusPick, String qRaw, Map<Integer, String> roomNumberByRoomId) {
        if (!"All".equalsIgnoreCase(statusPick)
                && !lease.GetStatus().equalsIgnoreCase(statusPick)) {
            return false;
        }
        if (qRaw.isEmpty()) {
            return true;
        }
        String tname = tenantLabel(lease.GetTenantID()).toLowerCase();
        String rnum = roomNumberByRoomId.getOrDefault(lease.GetRoomID(), "").toLowerCase();
        return tname.contains(qRaw) || rnum.contains(qRaw);
    }

    private String tenantLabel(int tenantId) {
        return tenantNameById.getOrDefault(tenantId, combineNameSafe(tenantId));
    }

    private String combineNameSafe(int tenantId) {
        Tenant t = tenantDao.GetByID(tenantId);
        return t == null ? ("Tenant#" + tenantId) : combineName(t);
    }

    private static String combineName(Tenant t) {
        return t.GetFirstName() + " " + t.GetLastName();
    }

    private VBox buildLeaseCard(Lease lease, String tenantName, String roomNum, boolean expiringSoon) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(12));
        card.setPrefWidth(220);
        card.setMinWidth(220);
        card.setMaxWidth(220);
        card.getStyleClass().addAll("card", "lease-card");
        if (expiringSoon) {
            card.getStyleClass().add("lease-card-expiring");
        }

        Label nameLabel = new Label(tenantName);
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);

        Label badge = leaseStatusBadge(lease);

        Label roomLabelUi = new Label("Room " + roomNum);
        roomLabelUi.getStyleClass().add("card-subtitle");

        Label range = new Label(lease.GetStartDate() + " → " + lease.GetEndDate());
        range.getStyleClass().add("card-subtitle");

        Label rentLbl = new Label(CurrencyUtil.format(lease.GetMonthlyRent()) + " / mo");
        rentLbl.getStyleClass().add("lease-rent");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("secondary-button");
        editBtn.setOnAction(ev -> handleEdit(lease));

        Button termBtn = new Button("Terminate");
        termBtn.getStyleClass().add("danger-button");
        termBtn.setOnAction(ev -> handleTerminate(lease));
        termBtn.setDisable(!"ACTIVE".equalsIgnoreCase(lease.GetStatus()));

        HBox actions = new HBox(8, editBtn, termBtn);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(nameLabel, badge, roomLabelUi, range, rentLbl, spacer, actions);
        return card;
    }

    private Label leaseStatusBadge(Lease lease) {
        String display = displayLeaseStatusText(lease);
        Label label = new Label(display);

        label.getStyleClass().addAll("status-badge", "lease-status-badge");
        if ("ACTIVE".equals(display)) {
            label.getStyleClass().add("badge-active");
        } else if ("PENDING".equals(display)) {
            label.getStyleClass().add("badge-pending");
        } else if ("TERMINATED".equals(display)) {
            label.getStyleClass().add("badge-terminated");
        } else if ("EXPIRED".equals(display)) {
            label.getStyleClass().add("badge-expired");
        }
        return label;
    }

    private static String displayLeaseStatusText(Lease lease) {
        String status = lease.GetStatus();
        if ("ACTIVE".equalsIgnoreCase(status) && lease.GetStartDate() != null
                && LocalDate.now().isBefore(lease.GetStartDate())) {
            return "PENDING";
        }
        return status != null ? status : "";
    }

    private void handleEdit(Lease lease) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_lease_dialog.fxml"));
            Parent root = loader.load();
            AddLeaseDialogController c = loader.getController();
            c.configureEdit(conn, lease, this::reloadFromDatabase);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit lease");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleTerminate(Lease lease) {
        if (!"ACTIVE".equalsIgnoreCase(lease.GetStatus())) {
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Terminate lease");
        confirm.setHeaderText(null);
        confirm.setContentText("Terminate lease #" + lease.GetLeaseID() + " for this tenant?");
        ButtonType yes = ButtonType.YES;
        ButtonType no = ButtonType.NO;
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(answer -> {
            if (answer == yes) {
                if (leaseDao.UpdateStatus(lease.GetLeaseID(), "TERMINATED")) {
                    roomDao.DecrementOccupancy(lease.GetRoomID());
                }
                reloadFromDatabase();
            }
        });
    }
}
