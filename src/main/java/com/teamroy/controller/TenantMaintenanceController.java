package com.teamroy.controller;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.teamroy.DatabaseUtility;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.entity.MaintenanceRequest;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class TenantMaintenanceController {

    @FXML
    private ComboBox<String> statusFilter;

    private Connection conn = DatabaseUtility.getConnection();
    private MaintenanceRequestDaoImpl dao = new MaintenanceRequestDaoImpl(conn);
    private int currentTenantId;

    @FXML
    private VBox requestList;

    @FXML
    public void initialize() {
        currentTenantId = SessionManager.getCurrentTenantId();

        statusFilter.getItems().addAll("All", "IN-PROGRESS", "RESOLVED");
        statusFilter.setValue("All");
        statusFilter.getStyleClass().add("payments-filter");

        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadMaintenanceRequests();
        });

        loadMaintenanceRequests();
    }

    private void loadMaintenanceRequests() {
        requestList.getChildren().clear();

        List<MaintenanceRequest> requests = dao.GetByTenantID(currentTenantId);
        String currentFilter = statusFilter.getValue();
        boolean hasVisibleRequests = false;

        for (MaintenanceRequest req : requests) {
            String status = req.GetStatus() != null ? req.GetStatus().toUpperCase() : "UNKNOWN";

            if (!"All".equalsIgnoreCase(currentFilter) && !status.equalsIgnoreCase(currentFilter)) {
                continue;
            }

            hasVisibleRequests = true;

            String ticketId = "REQ-" + req.GetRequestID();
            String desc = req.GetReportDescription() != null ? req.GetReportDescription() : "No description provided.";
            String room = getRoomNumberById(req.GetRoomID());
            String date = req.GetReportedDate() != null ? req.GetReportedDate().toLocalDate().toString()
                    : "Unknown Date";

            VBox card = createTicketCard(req.GetRequestID(), ticketId, date, room, desc, status);
            requestList.getChildren().add(card);
        }

        if (!hasVisibleRequests) {
            Label noRequests = new Label("No maintenance requests found.");
            noRequests.getStyleClass().add("card-subtitle");
            requestList.getChildren().add(noRequests);
        }
    }

    // --- DATABASE TRANSLATION HELPERS ---

    private Integer getRoomIdByNumber(String roomNumber) {
        String sql = "SELECT room_id FROM ROOM WHERE room_number = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roomNumber);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return rs.getInt("room_id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getRoomNumberById(Integer roomId) {
        if (roomId == null)
            return "General/Common Area";

        String sql = "SELECT room_number FROM ROOM WHERE room_id = ?";
        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, roomId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return "Room " + rs.getString("room_number");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unknown Room";
    }

    // --- DIALOG HANDLERS ---

    @FXML
    private void handleAddRequest() {
        Dialog<MaintenanceRequest> dialog = new Dialog<>();
        dialog.setTitle("New Maintenance Request");
        dialog.setHeaderText("Please describe the issue.");

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField roomNumberField = new TextField();
        roomNumberField.setPromptText("Optional (e.g., 101)");

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Describe the issue in detail...");
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefRowCount(4);

        grid.add(new Label("Room ID:"), 0, 0);
        grid.add(roomNumberField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descriptionArea, 1, 1);

        Node submitButton = dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);
        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> {
            submitButton.setDisable(newValue.trim().isEmpty());
        });

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                Integer roomId = null;
                String roomText = roomNumberField.getText().trim();
                if (!roomText.isEmpty()) {
                    roomId = getRoomIdByNumber(roomText);
                    if (roomId == null)
                        System.err.println("Room number '" + roomText + "' not found. Saving as NULL.");
                }
                return new MaintenanceRequest(
                        currentTenantId, roomId,
                        descriptionArea.getText().trim(),
                        LocalDateTime.now(), "NEW");
            }
            return null;
        });

        Optional<MaintenanceRequest> result = dialog.showAndWait();
        result.ifPresent(newRequest -> {
            dao.Create(newRequest);
            loadMaintenanceRequests();
        });
    }

    private void handleRemoveRequest(int requestId) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Cancel Maintenance Request");
        confirmDialog.setHeaderText("Are you sure you want to cancel this request?");
        confirmDialog.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            dao.Delete(requestId);
            loadMaintenanceRequests();
        }
    }

    // --- UI HELPERS ---

    private VBox createTicketCard(int requestId, String id, String date, String room, String description,
            String status) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.getStyleClass().add("ticket-card");

        // Top row: ID/room info, spacer, status badge, optional cancel button
        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        VBox idDateBox = new VBox(2);
        Label lblId = new Label(id + " \u2022 " + date);
        lblId.getStyleClass().add("card-subtitle");

        Label lblRoom = new Label(room);
        lblRoom.getStyleClass().add("announcement-title");

        idDateBox.getChildren().addAll(lblId, lblRoom);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().addAll("status-badge", getStatusClass(status));

        String statusColor = getStatusColor(status);
        lblStatus.setStyle("-fx-background-color: " + statusColor + "22; -fx-text-fill: " + statusColor
                + "; -fx-border-color: " + statusColor + ";");

        topRow.getChildren().addAll(idDateBox, spacer, lblStatus);

        if ("NEW".equalsIgnoreCase(status)) {
            Button btnRemove = new Button("Cancel Request");
            btnRemove.getStyleClass().add("cancel-request-btn");
            HBox.setMargin(btnRemove, new Insets(0, 0, 0, 10));
            btnRemove.setOnAction(e -> handleRemoveRequest(requestId));
            topRow.getChildren().add(btnRemove);
        }

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add("ticket-description");

        card.getChildren().addAll(topRow, lblDesc);
        return card;
    }

    private String getStatusClass(String status) {
        switch (status) {
            case "NEW":
                return "status-new";
            case "IN-PROGRESS":
                return "status-in-progress";
            case "RESOLVED":
                return "status-resolved";
            default:
                return "status-unknown";
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "NEW":
                return "#ef4444";
            case "IN-PROGRESS":
                return "#f59e0b";
            case "RESOLVED":
                return "#22c55e";
            default:
                return "#cbd5e1";
        }
    }
}