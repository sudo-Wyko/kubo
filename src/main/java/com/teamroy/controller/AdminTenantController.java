package com.teamroy.controller;

import com.teamroy.App;
import com.teamroy.DatabaseUtility;
import com.teamroy.model.dao.*;
import com.teamroy.model.entity.*;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

public class AdminTenantController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatus;

    // Table elements
    @FXML
    private TableView<Tenant> tenantTable;
    @FXML
    private TableColumn<Tenant, Integer> colTenantId;
    @FXML
    private TableColumn<Tenant, String> colFullName;
    @FXML
    private TableColumn<Tenant, String> colRoom;
    @FXML
    private TableColumn<Tenant, String> colStatus;

    // Side Panel elements
    @FXML
    private VBox sidePanel;
    @FXML
    private Label detailName;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailEmail;
    @FXML
    private Label detailPhone;
    @FXML
    private Label detailRoom;
    @FXML
    private Label detailBalance;
    @FXML
    private Label detailLeaseEnd;

    private ObservableList<Tenant> tenantList;
    private FilteredList<Tenant> filteredData;

    // DAOs
    private TenantDao tenantDao;
    private LeaseDao leaseDao;
    private RoomDao roomDao;

    @FXML
    public void initialize() {
        // 1. Initialize DAOs (Assuming you have a DatabaseConnection utility class)
        Connection conn = DatabaseUtility.getConnection();

        // 3. It's always best practice to check if the connection was successful!
        if (conn != null) {
            tenantDao = new TenantDaoImpl(conn);
            leaseDao = new LeaseDaoImpl(conn);
            roomDao = new RoomDaoImpl(conn);

            // 4. Load your data from the DB now that the DAOs are ready
            // loadTenantData();
        } else {
            System.err.println("Error: Database connection is null in AdminTenantController.");
        }

        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "Active", "Former"));
        filterStatus.setValue("All Statuses");

        // 2. Map Columns to Entity Properties
        colTenantId.setCellValueFactory(new PropertyValueFactory<>("tenantId"));

        // Custom mapping to combine First and Last Name
        colFullName.setCellValueFactory(cellData -> {
            Tenant t = cellData.getValue();
            return new SimpleStringProperty(t.GetFirstName() + " " + t.GetLastName());
        });

        // Custom mapping to fetch the Room Number via LeaseDao & RoomDao
        colRoom.setCellValueFactory(cellData -> {
            Lease activeLease = getActiveLeaseForTenant(cellData.getValue().GetTenantID());
            if (activeLease != null) {
                Room room = roomDao.GetByID(activeLease.GetRoomID());
                return new SimpleStringProperty(room != null ? room.GetRoomNumber() : "N/A");
            }
            return new SimpleStringProperty("Unassigned");
        });

        // Custom mapping to determine Status based on Lease
        colStatus.setCellValueFactory(cellData -> {
            Lease activeLease = getActiveLeaseForTenant(cellData.getValue().GetTenantID());
            return new SimpleStringProperty(activeLease != null ? "Active" : "Former");
        });

        tenantTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // 3. Load actual data from Database
        loadTableData();

        setupSearchAndFilter();
        setupTableSelection();
    }

    private void loadTableData() {
        // Fetch from Database instead of dummy data
        List<Tenant> dbTenants = tenantDao.GetAll();
        tenantList = FXCollections.observableArrayList(dbTenants);
        if (filteredData != null) {
            // If refreshing after an add, update the filtered list wrapper
            filteredData = new FilteredList<>(tenantList, b -> true);
            setupSearchAndFilter();
        }
    }

    private void setupTableSelection() {
        tenantTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showTenantDetails(newValue);
            }
        });
    }

    private void showTenantDetails(Tenant tenant) {
        detailName.setText(tenant.GetFirstName() + " " + tenant.GetLastName());
        detailEmail.setText("✉ " + tenant.GetEmail());
        detailPhone.setText("📞 " + tenant.GetContactNumber());
        detailBalance.setText("₱" + String.format("%.2f", tenant.GetTotalBalance()));

        // Fetch Lease info dynamically
        Lease activeLease = getActiveLeaseForTenant(tenant.GetTenantID());

        if (activeLease != null) {
            Room room = roomDao.GetByID(activeLease.GetRoomID());
            detailRoom.setText(room != null ? room.GetRoomNumber() : "N/A");
            detailLeaseEnd.setText(activeLease.GetEndDate().toString());

            detailStatus.setText("ACTIVE");
            detailStatus.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            detailRoom.setText("N/A");
            detailLeaseEnd.setText("Expired/None");

            detailStatus.setText("FORMER");
            detailStatus.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        sidePanel.setVisible(true);
        sidePanel.setManaged(true);
    }

    // Helper method using our LeaseDao to find if they are currently renting
    private Lease getActiveLeaseForTenant(int tenantId) {
        List<Lease> leases = leaseDao.GetByTenantID(tenantId);
        for (Lease lease : leases) {
            if ("ACTIVE".equalsIgnoreCase(lease.GetStatus())) {
                return lease;
            }
        }
        return null;
    }

    @FXML
    public void closeSidePanel() {
        sidePanel.setVisible(false);
        sidePanel.setManaged(false);
        tenantTable.getSelectionModel().clearSelection();
    }

    private void setupSearchAndFilter() {
        if (filteredData == null) {
            filteredData = new FilteredList<>(tenantList, b -> true);
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        SortedList<Tenant> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tenantTable.comparatorProperty());
        tenantTable.setItems(sortedData);
    }

    private void updatePredicate() {
        filteredData.setPredicate(tenant -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String statusFilter = filterStatus.getValue();

            String fullName = (tenant.GetFirstName() + " " + tenant.GetLastName()).toLowerCase();
            boolean matchesSearch = searchText.isEmpty() ||
                    fullName.contains(searchText) ||
                    tenant.GetEmail().toLowerCase().contains(searchText);

            // Determine current status via LeaseDao
            String actualStatus = getActiveLeaseForTenant(tenant.GetTenantID()) != null ? "Active" : "Former";
            boolean matchesStatus = statusFilter.equals("All Statuses") || actualStatus.equalsIgnoreCase(statusFilter);

            return matchesSearch && matchesStatus;
        });
    }

    @FXML
    public void handleAddTenant() {
        Dialog<Tenant> dialog = new Dialog<>();
        dialog.setTitle("Add New Tenant");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 50, 10, 10));

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("First Name");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Last Name");
        TextField contactField = new TextField();
        contactField.setPromptText("09XX-XXX-XXXX");
        TextField emailField = new TextField();
        emailField.setPromptText("email@domain.com");

        grid.add(new Label("First Name:"), 0, 0);
        grid.add(firstNameField, 1, 0);
        grid.add(new Label("Last Name:"), 0, 1);
        grid.add(lastNameField, 1, 1);
        grid.add(new Label("Contact:"), 0, 2);
        grid.add(contactField, 1, 2);
        grid.add(new Label("Email:"), 0, 3);
        grid.add(emailField, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (firstNameField.getText().isEmpty() || lastNameField.getText().isEmpty()
                        || contactField.getText().isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Name and Contact are required.").show();
                    return null;
                }

                // Create a real Tenant Entity
                Tenant newTenant = new Tenant();
                newTenant.SetFirstName(firstNameField.getText());
                newTenant.SetLastName(lastNameField.getText());
                newTenant.SetContactNumber(contactField.getText());
                newTenant.SetEmail(emailField.getText());
                newTenant.SetTotalBalance(0.0); // Starts with no debt

                return newTenant;
            }
            return null;
        });

        Optional<Tenant> result = dialog.showAndWait();
        result.ifPresent(tenant -> {
            // Save to database using our DAO
            tenantDao.Create(tenant);
            // Refresh table data from DB so it gets the generated ID
            loadTableData();
        });
    }

    @FXML
    public void switchToRooms() throws IOException {
        App.setRoot("admin");
    }

    @FXML
    public void switchToLeases() throws IOException {
        App.setRoot("admin_leases");
    }
}