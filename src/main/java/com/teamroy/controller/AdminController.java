package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
import com.teamroy.model.dao.RoomDao;
import com.teamroy.model.dao.RoomDaoImpl; // Uncomment when DB is ready
import com.teamroy.model.dao.MaintenanceRequestDao;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.MaintenanceRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.sql.Connection;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;

public class AdminController {

    // Top Bar Tools
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterType;
    @FXML
    private ComboBox<String> filterStatus;

    // Table
    @FXML
    private TableView<Room> roomTable;
    @FXML
    private TableColumn<Room, Integer> colRoomId;
    @FXML
    private TableColumn<Room, String> colRoomNumber;
    @FXML
    private TableColumn<Room, String> colRoomType;
    @FXML
    private TableColumn<Room, Integer> colCapacity;
    @FXML
    private TableColumn<Room, Integer> colOccupancy;
    @FXML
    private TableColumn<Room, String> colStatus;
    @FXML
    private TableColumn<Room, String> colMaintenance;

    private ObservableList<Room> roomList;
    private FilteredList<Room> filteredData;

    // DAO Instance
    private RoomDao roomDao;
    private MaintenanceRequestDao maintenanceDao;

    @FXML
    public void initialize() {
        // 1. Initialize DAOs (Assuming you have a DatabaseConnection utility class)
        Connection conn = DatabaseUtility.getConnection();

        // 3. It's always best practice to check if the connection was successful!
        if (conn != null) {
            roomDao = new RoomDaoImpl(conn);
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);

            // 4. Load your data from the DB now that the DAOs are ready
            // loadTenantData();
        } else {
            System.err.println("Error: Database connection is null in AdminTenantController.");
        }

        // 2. Setup Filters
        filterType.setItems(FXCollections.observableArrayList("All Types", "Single", "Double", "Dormitory"));
        filterType.setValue("All Types");

        filterStatus
                .setItems(FXCollections.observableArrayList("All Statuses", "Available", "Occupied", "Maintenance"));
        filterStatus.setValue("All Statuses");

        // 3. Link Columns to Model
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colOccupancy.setCellValueFactory(new PropertyValueFactory<>("currentOccupancy"));

        // Note: Assuming you added 'status' and 'maintenance' as transient/UI fields in
        // your Room POJO
        // Custom mapping for Status: Calculate based on Occupancy vs Capacity
        colStatus.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();
            if (room.GetCurrentOccupancy() >= room.GetCapacity()) {
                return new SimpleStringProperty("Occupied");
            } else {
                return new SimpleStringProperty("Available");
            }
        });

        // Custom mapping for Maintenance: Fetch from the Maintenance table dynamically
        colMaintenance.setCellValueFactory(cellData -> {
            Room room = cellData.getValue();

            if (maintenanceDao != null) {
                // Fetch all requests for this specific room
                List<MaintenanceRequest> requests = maintenanceDao.GetByRoomID(room.GetRoomID());

                // Loop through to see if any are NOT resolved
                for (MaintenanceRequest req : requests) {
                    if (!req.GetStatus().equalsIgnoreCase("RESOLVED")) {
                        // Return the description of the active issue (e.g., "Pending AC repair")
                        return new SimpleStringProperty(req.GetReportDescription());
                    }
                }
            }
            // If no active requests are found, it's all clear
            return new SimpleStringProperty("None");
        });

        // 4. Load Actual Data from Database (Using PascalCase DAO method)
        loadTableData();

        // 5. Setup Search and Filter Logic
        setupSearchAndFilter();
    }

    private void loadTableData() {
        // Fetch from Database instead of dummy data
        // List<Room> dbRooms = roomDao.GetAll();

        // Temporarily keeping null check in case DAO isn't hooked up yet
        if (roomDao != null) {
            List<Room> dbRooms = roomDao.GetAll();
            roomList = FXCollections.observableArrayList(dbRooms);
        } else {
            roomList = FXCollections.observableArrayList(); // Empty fallback
        }

        if (filteredData != null) {
            filteredData = new FilteredList<>(roomList, b -> true);
            setupSearchAndFilter();
        }
    }

    private void setupSearchAndFilter() {
        if (filteredData == null) {
            filteredData = new FilteredList<>(roomList, b -> true);
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterType.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        SortedList<Room> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(roomTable.comparatorProperty());
        roomTable.setItems(sortedData);
    }

    private void updatePredicate() {
        filteredData.setPredicate(room -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String type = filterType.getValue();
            String statusFilter = filterStatus.getValue();

            // 1. Check Search Bar
            boolean matchesSearch = searchText.isEmpty() || room.GetRoomNumber().toLowerCase().contains(searchText);

            // 2. Check Type Dropdown
            boolean matchesType = type.equals("All Types") || room.GetRoomType().equals(type);

            // 3. Calculate the actual status dynamically for the filter
            String actualStatus;
            if (room.GetCurrentOccupancy() >= room.GetCapacity()) {
                actualStatus = "Occupied";
            } else {
                actualStatus = "Available";
            }

            // Check Status Dropdown against our calculated actualStatus
            boolean matchesStatus = statusFilter.equals("All Statuses") || actualStatus.equals(statusFilter);

            return matchesSearch && matchesType && matchesStatus;
        });
    }

    @FXML
    public void handleAddRoom() {
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter details for the new room.");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomNumField = new TextField();
        roomNumField.setPromptText("e.g. 101A");
        TextField floorField = new TextField();
        floorField.setPromptText("Floor Number");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Single", "Double", "Dormitory"));
        typeBox.setValue("Single");
        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity");
        TextField priceField = new TextField();
        priceField.setPromptText("Monthly Price");

        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumField, 1, 0);
        grid.add(new Label("Floor:"), 0, 1);
        grid.add(floorField, 1, 1);
        grid.add(new Label("Room Type:"), 0, 2);
        grid.add(typeBox, 1, 2);
        grid.add(new Label("Capacity:"), 0, 3);
        grid.add(capacityField, 1, 3);
        grid.add(new Label("Price:"), 0, 4);
        grid.add(priceField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Room newRoom = new Room();
                    newRoom.SetRoomNumber(roomNumField.getText());
                    newRoom.SetFloor(Integer.parseInt(floorField.getText()));
                    newRoom.SetRoomType(typeBox.getValue());
                    newRoom.SetCapacity(Integer.parseInt(capacityField.getText()));
                    newRoom.SetPrice(Double.parseDouble(priceField.getText()));
                    newRoom.SetCurrentOccupancy(0);

                    // If your Room object has these UI fields, set them here
                    // newRoom.setStatus("Available");
                    // newRoom.setMaintenance("None");

                    return newRoom;
                } catch (NumberFormatException e) {
                    new Alert(Alert.AlertType.ERROR, "Floor, Capacity, and Price must be valid numbers.").show();
                    return null;
                }
            }
            return null;
        });

        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            if (roomDao != null) {
                // Use PascalCase for DAO Create method
                roomDao.Create(room);
                loadTableData(); // Refresh the table from the DB to get the auto-generated ID
            } else {
                roomList.add(room); // Fallback if DAO isn't connected yet
            }
        });
    }

    @FXML
    public void switchToLeases() throws IOException {
        App.setRoot("admin_leases");
    }

    @FXML
    public void switchToTenants() throws IOException {
        App.setRoot("admin_tenants");
    }
}