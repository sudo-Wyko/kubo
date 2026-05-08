package com.teamroy.controller;
import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
public class AdminMaintenanceController {
    @FXML
    private ComboBox<Room> roomFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private DatePicker dateFilterPicker;
    @FXML
    private VBox kanbanTotalBody;
    @FXML
    private VBox kanbanOpenBody;
    @FXML
    private VBox kanbanUrgentBody;
    @FXML
    private VBox kanbanCompletedBody;
    private Connection conn;
    private MaintenanceRequestDaoImpl maintenanceDao;
    private TenantDaoImpl tenantDao;
    private RoomDaoImpl roomDao;
    private final Map<Integer, String> tenantNameCache = new HashMap<>();
    private Room createAllRoomsOption() {
        Room sentinel = new Room();
        sentinel.SetRoomID(-1);
        sentinel.SetRoomNumber("All rooms");
        return sentinel;
    }
    @FXML
    private void initialize() {
        try {
            conn = ConnectionManager.getConnection();
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            roomDao = new RoomDaoImpl(conn);
        } catch (Exception ex) {
            System.err.println("Failed to initialize maintenance view: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }
        roomFilterCombo.getItems().add(createAllRoomsOption());
        roomFilterCombo.getItems().addAll(roomDao.GetAll());
        roomFilterCombo.getSelectionModel().selectFirst();
        bindRoomFilterCells();
        statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "NEW", "IN-PROGRESS", "RESOLVED"));
        statusFilterCombo.getSelectionModel().selectFirst();
        roomFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> reloadKanban());
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> reloadKanban());
        dateFilterPicker.valueProperty().addListener((obs, o, n) -> reloadKanban());
        reloadKanban();
    }
    private void bindRoomFilterCells() {
        roomFilterCombo.setCellFactory(lv -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetRoomID() < 0) {
                    setText("All rooms");
                } else {
                    setText(item.GetRoomNumber());
                }
            }
        });
        roomFilterCombo.setButtonCell(new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetRoomID() < 0) {
                    setText("All rooms");
                } else {
                    setText(item.GetRoomNumber());
                }
            }
        });
    }
    private boolean isUrgent(MaintenanceRequest mr) {
        return "NEW".equalsIgnoreCase(mr.GetStatus())
                && mr.GetReportedDate() != null
                && mr.GetReportedDate().isBefore(LocalDateTime.now().minusDays(3));
    }
    private boolean isResolved(MaintenanceRequest mr) {
        return "RESOLVED".equalsIgnoreCase(mr.GetStatus());
    }
    private boolean isInProgress(MaintenanceRequest mr) {
        String status = mr.GetStatus();
        return "IN-PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status);
    }
    private List<MaintenanceRequest> applyFilters(List<MaintenanceRequest> source) {
        List<MaintenanceRequest> list = new ArrayList<>(source);
        Room roomSelection = roomFilterCombo.getSelectionModel().getSelectedItem();
        if (roomSelection != null && roomSelection.GetRoomID() >= 0) {
            final int roomId = roomSelection.GetRoomID();
            list = list.stream()
                    .filter(mr -> mr.GetRoomID() != null && mr.GetRoomID() == roomId)
                    .collect(Collectors.toList());
        }
        String statusSelection = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (statusSelection != null && !"ALL".equalsIgnoreCase(statusSelection)) {
            list = list.stream()
                    .filter(mr -> statusSelection.equalsIgnoreCase(mr.GetStatus()))
                    .collect(Collectors.toList());
        }
        LocalDate dateSelection = dateFilterPicker.getValue();
        if (dateSelection != null) {
            list = list.stream().filter(mr -> {
                LocalDateTime reported = mr.GetReportedDate();
                return reported != null && reported.toLocalDate().isEqual(dateSelection);
            }).collect(Collectors.toList());
        }
        return list;
    }
    private void refreshTenantNames() {
        tenantNameCache.clear();
        for (Tenant tenant : tenantDao.GetAll()) {
            tenantNameCache.put(tenant.GetTenantID(),
                    tenant.GetFirstName() + " " + tenant.GetLastName());
        }
    }
    private String resolveRoomLabel(MaintenanceRequest mr) {
        if (mr.GetRoomID() == null) {
            return "Shared Area";
        }
        Room room = roomDao.GetByID(mr.GetRoomID());
        if (room == null) {
            return "Room #" + mr.GetRoomID();
        }
        return room.GetRoomNumber();
    }
    private String truncateDescription(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.replace('\n', ' ').trim();
        if (normalized.length() <= 140) {
            return normalized;
        }
        return normalized.substring(0, 137) + "...";
    }
    private Label buildStatusBadge(String status) {
        Label badge = new Label(status);
        String color = "#334155";
        if ("NEW".equalsIgnoreCase(status)) {
            color = "#2563eb";
        } else if ("IN-PROGRESS".equalsIgnoreCase(status) || "IN_PROGRESS".equalsIgnoreCase(status)) {
            color = "#ca8a04";
        } else if ("RESOLVED".equalsIgnoreCase(status)) {
            color = "#15803d";
        }
        badge.setStyle("-fx-background-color: " + color
                + "; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 10;");
        return badge;
    }
    private VBox buildCard(MaintenanceRequest mr) {
        VBox card = new VBox(8);
        card.setStyle(
                "-fx-border-color: #334155; -fx-border-radius: 8; -fx-background-color: #111827; -fx-background-radius: 8; -fx-padding: 12;");
        Label title = new Label("Request #" + mr.GetRequestID());
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        String tenantName = tenantNameCache.getOrDefault(mr.GetTenantID(), "Tenant #" + mr.GetTenantID());
        Label tenantLabel = new Label(tenantName);
        tenantLabel.setStyle("-fx-text-fill: #cbd5f5;");
        Label roomLabel = new Label(resolveRoomLabel(mr));
        roomLabel.setStyle("-fx-text-fill: #94a3b8;");
        Label description = new Label(truncateDescription(mr.GetReportDescription()));
        description.setWrapText(true);
        description.setMaxHeight(48);
        description.setStyle("-fx-text-fill: #e2e8f0;");
        String dateText = mr.GetReportedDate() == null ? "â€”"
                : mr.GetReportedDate().toLocalDate().toString();
        Label dateLabel = new Label("Reported: " + dateText);
        dateLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        HBox statusRow = new HBox(10);
        statusRow.getChildren().add(buildStatusBadge(mr.GetStatus()));
        ComboBox<String> statusPicker = new ComboBox<>(
                FXCollections.observableArrayList("NEW", "IN-PROGRESS", "RESOLVED"));
        statusPicker.setValue(mr.GetStatus());
        statusPicker.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (oldVal == null || newVal == null || newVal.equals(oldVal)) {
                return;
            }
            maintenanceDao.UpdateStatus(mr.GetRequestID(), newVal);
            reloadKanban();
        });
        statusRow.getChildren().add(statusPicker);
        card.getChildren().addAll(title, tenantLabel, roomLabel, description, dateLabel, statusRow);
        return card;
    }
    private void populateColumn(VBox body, List<MaintenanceRequest> items) {
        body.getChildren().clear();
        for (MaintenanceRequest mr : items) {
            body.getChildren().add(buildCard(mr));
        }
    }
    private void reloadKanban() {
        if (maintenanceDao == null) {
            return;
        }
        refreshTenantNames();
        List<MaintenanceRequest> filtered = applyFilters(maintenanceDao.GetAll());
        List<MaintenanceRequest> urgent = filtered.stream().filter(this::isUrgent).collect(Collectors.toList());
        List<MaintenanceRequest> completed = filtered.stream().filter(this::isResolved).collect(Collectors.toList());
        List<MaintenanceRequest> open = filtered.stream()
                .filter(mr -> !isResolved(mr))
                .filter(mr -> isInProgress(mr) || ("NEW".equalsIgnoreCase(mr.GetStatus()) && !isUrgent(mr)))
                .collect(Collectors.toList());
        populateColumn(kanbanTotalBody, filtered);
        populateColumn(kanbanOpenBody, open);
        populateColumn(kanbanUrgentBody, urgent);
        populateColumn(kanbanCompletedBody, completed);
    }
}
