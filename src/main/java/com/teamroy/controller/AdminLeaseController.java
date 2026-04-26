package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
import com.teamroy.model.dao.*;
import com.teamroy.model.entity.*;

import java.io.IOException;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.StringConverter;

public class AdminLeaseController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatus;

    // The container for our cards instead of a TableView
    @FXML
    private FlowPane leaseCardsContainer;

    private ObservableList<Lease> leaseList;
    private FilteredList<Lease> filteredData;

    // Our DAO Interfaces
    private LeaseDao leaseDao;
    private TenantDao tenantDao;
    private RoomDao roomDao;

    @FXML
    public void initialize() {
        // 1. Grab the global database connection and initialize DAOs!
        Connection conn = DatabaseUtility.getConnection();
        if (conn != null) {
            leaseDao = new LeaseDaoImpl(conn);
            tenantDao = new TenantDaoImpl(conn);
            roomDao = new RoomDaoImpl(conn);
        } else {
            System.err.println("Failed to connect to the database in AdminLeaseController.");
        }

        // 2. Setup the UI Filter
        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "Active", "Pending", "Expired"));
        filterStatus.setValue("All Statuses");

        // 3. Load Real Data
        loadLeaseData();
        setupSearchAndFilter();
    }

    private void loadLeaseData() {
        if (leaseDao != null) {
            List<Lease> dbLeases = leaseDao.GetAll();
            leaseList = FXCollections.observableArrayList(dbLeases);
        } else {
            leaseList = FXCollections.observableArrayList();
        }

        if (filteredData != null) {
            filteredData = new FilteredList<>(leaseList, b -> true);
            renderCards();
        }
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(leaseList, b -> true);

        // Listen for changes
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        // Listen to the main list for additions/removals
        filteredData.addListener((javafx.collections.ListChangeListener.Change<? extends Lease> c) -> {
            renderCards();
        });

        // Initial render
        renderCards();
    }

    private void updatePredicate() {
        filteredData.setPredicate(lease -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String status = filterStatus.getValue();

            // Fetch actual names/numbers from DB using the IDs stored in the Lease
            Tenant tenant = tenantDao != null ? tenantDao.GetByID(lease.GetTenantID()) : null;
            Room room = roomDao != null ? roomDao.GetByID(lease.GetRoomID()) : null;

            String tenantName = tenant != null ? (tenant.GetFirstName() + " " + tenant.GetLastName()).toLowerCase()
                    : "";
            String roomNum = room != null ? room.GetRoomNumber().toLowerCase() : "";

            boolean matchesSearch = searchText.isEmpty() ||
                    tenantName.contains(searchText) ||
                    roomNum.contains(searchText);

            boolean matchesStatus = status.equals("All Statuses") ||
                    (lease.GetStatus() != null && lease.GetStatus().equalsIgnoreCase(status));

            return matchesSearch && matchesStatus;
        });

        renderCards();
    }

    private void renderCards() {
        leaseCardsContainer.getChildren().clear();
        for (Lease lease : filteredData) {
            leaseCardsContainer.getChildren().add(createLeaseCard(lease));
        }
    }

    private VBox createLeaseCard(Lease lease) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 0);");

        // Lookup related entities
        Tenant tenant = tenantDao != null ? tenantDao.GetByID(lease.GetTenantID()) : null;
        Room room = roomDao != null ? roomDao.GetByID(lease.GetRoomID()) : null;

        String displayName = tenant != null ? tenant.GetFirstName() + " " + tenant.GetLastName() : "Unknown Tenant";
        String displayRoom = room != null ? room.GetRoomNumber() : "Unknown";

        // Top Row: Tenant Name and Status Badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(displayName);
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label statusLabel = new Label(lease.GetStatus() != null ? lease.GetStatus().toUpperCase() : "UNKNOWN");
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12;");

        if ("ACTIVE".equalsIgnoreCase(lease.GetStatus())) {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
        } else if ("PENDING".equalsIgnoreCase(lease.GetStatus())) {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #fef08a; -fx-text-fill: #854d0e;");
        } else {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(nameLabel, spacer, statusLabel);

        // Details
        Label roomLabel = new Label("Room: " + displayRoom);
        roomLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Label datesLabel = new Label(lease.GetStartDate() + " to " + lease.GetEndDate());
        datesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Bottom Row: Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-cursor: hand;");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setOnAction(e -> handleEdit(lease, displayName));

        Button renewBtn = new Button("Renew");
        renewBtn.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-cursor: hand;");
        renewBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(renewBtn, Priority.ALWAYS);
        renewBtn.setOnAction(e -> handleRenew(lease, displayName));

        actionBox.getChildren().addAll(editBtn, renewBtn);
        card.getChildren().addAll(topRow, roomLabel, datesLabel, new Separator(), actionBox);
        return card;
    }

    private void handleEdit(Lease lease, String tenantName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Editing lease for: " + tenantName);
        alert.setHeaderText("Edit Feature");
        alert.show();
    }

    private void handleRenew(Lease lease, String tenantName) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Renewing lease for: " + tenantName);
        alert.setHeaderText("Renew Feature");
        alert.show();
    }

    @FXML
    public void handleAddLease() {
        Dialog<Lease> dialog = new Dialog<>();
        dialog.setTitle("Add New Lease Agreement");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        // Use ComboBoxes to fetch Real Entities from Database
        ComboBox<Tenant> tenantBox = new ComboBox<>();
        if (tenantDao != null) {
            tenantBox.getItems().addAll(tenantDao.GetAll());
        }
        tenantBox.setConverter(new StringConverter<Tenant>() {
            @Override
            public String toString(Tenant t) {
                return t == null ? "" : t.GetFirstName() + " " + t.GetLastName();
            }

            @Override
            public Tenant fromString(String s) {
                return null;
            }
        });
        tenantBox.setPromptText("Select Tenant");

        ComboBox<Room> roomBox = new ComboBox<>();
        if (roomDao != null) {
            roomBox.getItems().addAll(roomDao.GetAll());
        }
        roomBox.setConverter(new StringConverter<Room>() {
            @Override
            public String toString(Room r) {
                return r == null ? "" : r.GetRoomNumber() + " (₱" + r.GetPrice() + ")";
            }

            @Override
            public Room fromString(String s) {
                return null;
            }
        });
        roomBox.setPromptText("Select Room");

        DatePicker startDatePicker = new DatePicker();
        DatePicker endDatePicker = new DatePicker();

        grid.add(new Label("Tenant:"), 0, 0);
        grid.add(tenantBox, 1, 0);
        grid.add(new Label("Room:"), 0, 1);
        grid.add(roomBox, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2);
        grid.add(startDatePicker, 1, 2);
        grid.add(new Label("End Date:"), 0, 3);
        grid.add(endDatePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null ||
                        tenantBox.getValue() == null || roomBox.getValue() == null) {
                    new Alert(Alert.AlertType.ERROR, "Please fill out all fields and select from the dropdowns.")
                            .show();
                    return null;
                }

                // Map the UI selections to a real Lease Entity
                Lease newLease = new Lease();
                newLease.SetTenantID(tenantBox.getValue().GetTenantID());
                newLease.SetRoomID(roomBox.getValue().GetRoomID());
                newLease.SetStartDate(startDatePicker.getValue());
                newLease.SetEndDate(endDatePicker.getValue());
                newLease.SetMonthlyRent(roomBox.getValue().GetPrice()); // Pull price from Room mapping
                newLease.SetStatus("Pending");

                return newLease;
            }
            return null;
        });

        Optional<Lease> result = dialog.showAndWait();
        result.ifPresent(lease -> {
            if (leaseDao != null) {
                leaseDao.Create(lease); // Save to MySQL
                loadLeaseData(); // Refresh UI
            }
        });
    }

    @FXML
    public void switchToRooms() throws IOException {
        App.setRoot("admin");
    }

    @FXML
    public void switchToTenants() throws IOException {
        App.setRoot("admin_tenants");
    }
}