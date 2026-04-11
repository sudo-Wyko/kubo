package com.teamroy.Controller;

import com.teamroy.App;
import java.io.IOException;
import com.teamroy.Model.Room;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import java.util.Optional;

public class AdminController {

    // Top Bar Tools
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterType;
    @FXML private ComboBox<String> filterStatus;

    // Table
    @FXML private TableView<Room> roomTable;
    @FXML private TableColumn<Room, Integer> colRoomId;
    @FXML private TableColumn<Room, String> colRoomNumber;
    @FXML private TableColumn<Room, String> colRoomType;
    @FXML private TableColumn<Room, Integer> colCapacity;
    @FXML private TableColumn<Room, Integer> colOccupancy;
    @FXML private TableColumn<Room, String> colStatus;
    @FXML private TableColumn<Room, String> colMaintenance;

    private ObservableList<Room> roomList;
    private FilteredList<Room> filteredData;

    @FXML
    public void initialize() {
        // 1. Setup Filters
        filterType.setItems(FXCollections.observableArrayList("All Types", "Single", "Double", "Dormitory"));
        filterType.setValue("All Types");

        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "Available", "Occupied", "Maintenance"));
        filterStatus.setValue("All Statuses");

        // 2. Link Columns to Model
        colRoomId.setCellValueFactory(new PropertyValueFactory<>("roomId"));
        colRoomNumber.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colRoomType.setCellValueFactory(new PropertyValueFactory<>("roomType"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colOccupancy.setCellValueFactory(new PropertyValueFactory<>("currentOccupancy"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colMaintenance.setCellValueFactory(new PropertyValueFactory<>("maintenance"));

        // 3. Load Dummy Data
        roomList = FXCollections.observableArrayList(
                new Room(1, "101A", "Single", 1, 0, "Available", "None"),
                new Room(2, "102B", "Double", 2, 2, "Occupied", "None"),
                new Room(3, "201A", "Dormitory", 4, 1, "Available", "Pending AC repair"),
                new Room(4, "202B", "Single", 1, 0, "Maintenance", "Leaking pipe")
        );

        // 4. Setup Search and Filter Logic
        setupSearchAndFilter();
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(roomList, b -> true);

        // Listen for changes in the search bar or dropdowns
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterType.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        // Wrap the FilteredList in a SortedList (so column clicking still sorts)
        SortedList<Room> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(roomTable.comparatorProperty());
        roomTable.setItems(sortedData);
    }

    private void updatePredicate() {
        filteredData.setPredicate(room -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String type = filterType.getValue();
            String status = filterStatus.getValue();

            // Check Search Bar (Matches Room Number)
            boolean matchesSearch = searchText.isEmpty() || room.getRoomNumber().toLowerCase().contains(searchText);

            // Check Type Filter
            boolean matchesType = type.equals("All Types") || room.getRoomType().equals(type);

            // Check Status Filter
            boolean matchesStatus = status.equals("All Statuses") || room.getStatus().equals(status);

            return matchesSearch && matchesType && matchesStatus;
        });
    }

    @FXML
    public void handleAddRoom() {
        // 1. Create the Pop-up Window (Dialog)
        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter details for the new dormitory room.");

        // 2. Set the button types (Save and Cancel)
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // 3. Create the input fields for the popup
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomNumField = new TextField();
        roomNumField.setPromptText("Room Number");

        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Single", "Double", "Dormitory"));
        typeBox.setValue("Single");

        TextField capacityField = new TextField();
        capacityField.setPromptText("Capacity");

        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(roomNumField, 1, 0);
        grid.add(new Label("Room Type:"), 0, 1);
        grid.add(typeBox, 1, 1);
        grid.add(new Label("Capacity:"), 0, 2);
        grid.add(capacityField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // 4. Convert the result to a Room object when "Save" is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    int cap = Integer.parseInt(capacityField.getText());
                    int fakeId = roomList.size() + 1;
                    // Default newly created rooms to Available with no maintenance
                    return new Room(fakeId, roomNumField.getText(), typeBox.getValue(), cap, 0, "Available", "None");
                } catch (NumberFormatException e) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Capacity must be a number.");
                    alert.show();
                    return null;
                }
            }
            return null;
        });

        // 5. Show the window and wait for the user. If they saved a room, add it to the table!
        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> roomList.add(room));
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