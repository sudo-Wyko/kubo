package com.teamroy.Controller;

import com.teamroy.App;
import java.io.IOException;
import com.teamroy.Model.Tenant;
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
import java.util.Optional;

public class AdminTenantController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;

    // Table elements
    @FXML private TableView<Tenant> tenantTable;
    @FXML private TableColumn<Tenant, Integer> colTenantId;
    @FXML private TableColumn<Tenant, String> colFullName;
    @FXML private TableColumn<Tenant, String> colRoom;
    @FXML private TableColumn<Tenant, String> colStatus;

    // Side Panel elements
    @FXML private VBox sidePanel;
    @FXML private Label detailName;
    @FXML private Label detailStatus;
    @FXML private Label detailEmail;
    @FXML private Label detailPhone;
    @FXML private Label detailRoom;
    @FXML private Label detailBalance;
    @FXML private Label detailLeaseEnd;

    private ObservableList<Tenant> tenantList;
    private FilteredList<Tenant> filteredData;

    @FXML
    public void initialize() {
        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "Active", "Former"));
        filterStatus.setValue("All Statuses");

        // Link columns
        colTenantId.setCellValueFactory(new PropertyValueFactory<>("tenantId"));
        colFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tenantTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Dummy Data
        tenantList = FXCollections.observableArrayList(
                new Tenant(201, "Nyko Wyne", "0917 123 4567", "wyko@email.com", "Active", "101A", 0.00, "May 31, 2026"),
                new Tenant(202, "Xapantipi Raphael", "0918 987 6543", "xapantipi@email.com", "Active", "102B", 4500.00, "Sep 15, 2026"),
                new Tenant(203, "Michael Ken", "0919 555 0000", "kenji@email.com", "Active", "201A", 0.00, "Mar 31, 2027"),
                new Tenant(204, "James Lebron", "0920 111 2222", "thegoat@email.com", "Former", "N/A", 0.00, "Expired")
        );

        setupSearchAndFilter();
        setupTableSelection();
    }

    private void setupTableSelection() {
        // Listen for when the user clicks a row in the table
        tenantTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                showTenantDetails(newValue);
            }
        });
    }

    private void showTenantDetails(Tenant tenant) {
        // Fill the labels with the selected tenant's data
        detailName.setText(tenant.getFullName());
        detailEmail.setText("✉ " + tenant.getEmail());
        detailPhone.setText("📞 " + tenant.getContactNumber());
        detailRoom.setText(tenant.getRoomNumber());
        detailBalance.setText("₱" + String.format("%.2f", tenant.getBalance()));
        detailLeaseEnd.setText(tenant.getLeaseEndDate());

        detailStatus.setText(tenant.getStatus().toUpperCase());
        if(tenant.getStatus().equals("Active")) {
            detailStatus.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            detailStatus.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 3 10; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        }

        // Make the panel visible and tell JavaFX to allocate layout space for it
        sidePanel.setVisible(true);
        sidePanel.setManaged(true);
    }

    @FXML
    public void closeSidePanel() {
        // Hide the panel and clear the table selection
        sidePanel.setVisible(false);
        sidePanel.setManaged(false);
        tenantTable.getSelectionModel().clearSelection();
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(tenantList, b -> true);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        SortedList<Tenant> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tenantTable.comparatorProperty());
        tenantTable.setItems(sortedData);
    }

    private void updatePredicate() {
        filteredData.setPredicate(tenant -> {
            String searchText = searchField.getText() == null ? "" : searchField.getText().toLowerCase();
            String status = filterStatus.getValue();

            boolean matchesSearch = searchText.isEmpty() ||
                    tenant.getFullName().toLowerCase().contains(searchText) ||
                    tenant.getEmail().toLowerCase().contains(searchText);

            boolean matchesStatus = status.equals("All Statuses") || tenant.getStatus().equals(status);

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
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 50, 10, 10));

        TextField nameField = new TextField(); nameField.setPromptText("e.g. John Doe");
        TextField contactField = new TextField(); contactField.setPromptText("09XX-XXX-XXXX");
        TextField emailField = new TextField(); emailField.setPromptText("email@domain.com");

        grid.add(new Label("Full Name:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Contact:"), 0, 1); grid.add(contactField, 1, 1);
        grid.add(new Label("Email:"), 0, 2); grid.add(emailField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().isEmpty() || contactField.getText().isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Name and Contact are required.").show();
                    return null;
                }
                int fakeId = 200 + tenantList.size() + 1;
                // Newly added tenants start with no room assigned and 0 balance
                return new Tenant(fakeId, nameField.getText(), contactField.getText(), emailField.getText(), "Active", "Pending", 0.0, "TBD");
            }
            return null;
        });

        Optional<Tenant> result = dialog.showAndWait();
        result.ifPresent(tenant -> tenantList.add(tenant));
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