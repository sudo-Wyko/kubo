package com.teamroy.controller;

import com.teamroy.SessionManager;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class TenantMaintenanceController {

    @FXML
    private TextArea issueDescriptionArea;
    @FXML
    private ComboBox<Room> roomCombo;
    @FXML
    private Label submitErrorLabel;
    @FXML
    private TableView<MaintenanceRequest> requestsTable;
    @FXML
    private TableColumn<MaintenanceRequest, String> colDesc;
    @FXML
    private TableColumn<MaintenanceRequest, String> colRoom;
    @FXML
    private TableColumn<MaintenanceRequest, String> colWhen;
    @FXML
    private TableColumn<MaintenanceRequest, Label> colStatus;

    private final Map<Integer, String> roomLookup = new HashMap<>();

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

    private Room createSharedAreaChoice() {
        Room sentinel = new Room();
        sentinel.SetRoomID(0);
        sentinel.SetRoomNumber("Shared area");
        return sentinel;
    }

    private void bindRoomCells() {
        roomCombo.setCellFactory(lv -> new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetRoomID() <= 0) {
                    setText("Shared area (optional)");
                } else {
                    setText(item.GetRoomNumber());
                }
            }
        });
        roomCombo.setButtonCell(new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else if (item.GetRoomID() <= 0) {
                    setText("Shared area (optional)");
                } else {
                    setText(item.GetRoomNumber());
                }
            }
        });
    }

    private Label buildStatusBadge(String status) {
        Label label = new Label(status == null ? "" : status);
        String bg = "#334155";
        if ("NEW".equalsIgnoreCase(status)) {
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

    private void configureTable() {
        colDesc.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().GetReportDescription()));

        colRoom.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(resolveRoomLabel(cd.getValue())));

        colWhen.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                cd.getValue().GetReportedDate() == null ? ""
                        : cd.getValue().GetReportedDate().toLocalDate().toString()));

        colStatus.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(buildStatusBadge(cd.getValue().GetStatus())));
        colStatus.setCellFactory(column -> new TableCell<MaintenanceRequest, Label>() {
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

    private String resolveRoomLabel(MaintenanceRequest mr) {
        if (mr.GetRoomID() == null) {
            return "Shared Area";
        }
        return roomLookup.getOrDefault(mr.GetRoomID(), "Room #" + mr.GetRoomID());
    }

    @FXML
    private void initialize() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            submitErrorLabel.setText("Tenant session unavailable.");
            submitErrorLabel.setVisible(true);
            submitErrorLabel.setManaged(true);
            return;
        }

        bindRoomCells();
        configureTable();

        try (Connection conn = getConnection()) {
            RoomDaoImpl roomDao = new RoomDaoImpl(conn);
            roomCombo.getItems().clear();
            roomCombo.getItems().add(createSharedAreaChoice());
            List<Room> rooms = roomDao.GetAll();
            roomCombo.getItems().addAll(rooms);

            roomLookup.clear();
            for (Room room : rooms) {
                roomLookup.put(room.GetRoomID(), room.GetRoomNumber());
            }

            roomCombo.getSelectionModel().selectFirst();

            MaintenanceRequestDaoImpl maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            List<MaintenanceRequest> rows = maintenanceDao.GetByTenantID(tenant.GetTenantID());
            requestsTable.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception ex) {
            ex.printStackTrace();
            submitErrorLabel.setText("Could not load maintenance data.");
            submitErrorLabel.setVisible(true);
            submitErrorLabel.setManaged(true);
        }
    }

    @FXML
    private void handleSubmitRequest() {
        Tenant tenant = SessionManager.getCurrentTenant();
        submitErrorLabel.setVisible(false);
        submitErrorLabel.setManaged(false);

        if (tenant == null) {
            submitErrorLabel.setText("Tenant session unavailable.");
            submitErrorLabel.setVisible(true);
            submitErrorLabel.setManaged(true);
            return;
        }

        String description = issueDescriptionArea.getText();
        if (description == null || description.isBlank()) {
            submitErrorLabel.setText("Describe the issue before submitting.");
            submitErrorLabel.setVisible(true);
            submitErrorLabel.setManaged(true);
            return;
        }

        Room selected = roomCombo.getSelectionModel().getSelectedItem();
        Integer roomId = null;
        if (selected != null && selected.GetRoomID() > 0) {
            roomId = selected.GetRoomID();
        }

        try (Connection conn = getConnection()) {
            MaintenanceRequestDaoImpl maintenanceDao = new MaintenanceRequestDaoImpl(conn);
            MaintenanceRequest request = new MaintenanceRequest(
                    tenant.GetTenantID(),
                    roomId,
                    description.trim(),
                    LocalDateTime.now(),
                    "NEW");
            maintenanceDao.Create(request);

            issueDescriptionArea.clear();

            List<MaintenanceRequest> rows = maintenanceDao.GetByTenantID(tenant.GetTenantID());
            requestsTable.setItems(FXCollections.observableArrayList(rows));
            requestsTable.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
            submitErrorLabel.setText("Could not submit request.");
            submitErrorLabel.setVisible(true);
            submitErrorLabel.setManaged(true);
        }
    }
}
