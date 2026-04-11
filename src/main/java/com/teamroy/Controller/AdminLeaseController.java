package com.teamroy.Controller;

import com.teamroy.App;
import java.io.IOException;
import com.teamroy.Model.Lease;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminLeaseController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterStatus;

    // The container for our cards instead of a TableView
    @FXML private FlowPane leaseCardsContainer;

    private ObservableList<Lease> leaseList;
    private FilteredList<Lease> filteredData;

    @FXML
    public void initialize() {
        filterStatus.setItems(FXCollections.observableArrayList("All Statuses", "Active", "Pending", "Expired"));
        filterStatus.setValue("All Statuses");

        // Load Dummy Data
        leaseList = FXCollections.observableArrayList(
                new Lease(1001, "Nyko Wyne", "101A", "2025-08-01", "2026-05-31", "Active"),
                new Lease(1002, "Xapantipi Raphael", "102B", "2025-09-15", "2026-09-15", "Active"),
                new Lease(1003, "Michael Ken", "201A", "2026-04-01", "2027-03-31", "Pending"),
                new Lease(1004, "James Lebron", "202B", "2024-01-01", "2024-12-31", "Expired")
        );

        setupSearchAndFilter();
    }

    private void setupSearchAndFilter() {
        filteredData = new FilteredList<>(leaseList, b -> true);

        // Listen for changes
        searchField.textProperty().addListener((observable, oldValue, newValue) -> updatePredicate());
        filterStatus.valueProperty().addListener((observable, oldValue, newValue) -> updatePredicate());

        // Listen to the main list for additions (when we add a new lease)
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

            boolean matchesSearch = searchText.isEmpty() ||
                    lease.getTenantName().toLowerCase().contains(searchText) ||
                    lease.getRoomNumber().toLowerCase().contains(searchText);

            boolean matchesStatus = status.equals("All Statuses") || lease.getStatus().equals(status);

            return matchesSearch && matchesStatus;
        });

        // Redraw cards whenever filter changes
        renderCards();
    }

    private void renderCards() {
        leaseCardsContainer.getChildren().clear(); // Clear old cards
        for (Lease lease : filteredData) {
            leaseCardsContainer.getChildren().add(createLeaseCard(lease)); // Add new cards
        }
    }

    private VBox createLeaseCard(Lease lease) {
        VBox card = new VBox(12); // 12px vertical spacing
        card.setPadding(new Insets(20));
        card.setPrefWidth(280);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 0);");

        // Top Row: Tenant Name and Status Badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label nameLabel = new Label(lease.getTenantName());
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label statusLabel = new Label(lease.getStatus());
        statusLabel.setPadding(new Insets(3, 8, 3, 8));
        statusLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 12;");

        // Color code the status
        if (lease.getStatus().equals("Active")) {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
        } else if (lease.getStatus().equals("Pending")) {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #fef08a; -fx-text-fill: #854d0e;");
        } else {
            statusLabel.setStyle(statusLabel.getStyle() + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        topRow.getChildren().addAll(nameLabel, spacer, statusLabel);

        // Details
        Label roomLabel = new Label("Room: " + lease.getRoomNumber());
        roomLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #475569;");

        Label datesLabel = new Label(lease.getStartDate() + " to " + lease.getEndDate());
        datesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #64748b;");

        // Bottom Row: Action Buttons
        HBox actionBox = new HBox(10);
        actionBox.setPadding(new Insets(10, 0, 0, 0));

        Button editBtn = new Button("Edit");
        editBtn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-cursor: hand;");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setOnAction(e -> handleEdit(lease)); // Hook up the edit action

        Button renewBtn = new Button("Renew");
        renewBtn.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; -fx-cursor: hand;");
        renewBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(renewBtn, Priority.ALWAYS);
        renewBtn.setOnAction(e -> handleRenew(lease)); // Hook up the renew action

        actionBox.getChildren().addAll(editBtn, renewBtn);

        // Assemble the card
        card.getChildren().addAll(topRow, roomLabel, datesLabel, new Separator(), actionBox);
        return card;
    }

    // --- BUTTON ACTIONS ---
    private void handleEdit(Lease lease) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Editing lease for: " + lease.getTenantName());
        alert.setHeaderText("Edit Feature");
        alert.show();
        // Later, you can open a Dialog similar to handleAddLease but pre-filled with data.
    }

    private void handleRenew(Lease lease) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Renewing lease for: " + lease.getTenantName());
        alert.setHeaderText("Renew Feature");
        alert.show();
        // Add your renewal logic here later!
    }

    @FXML
    public void handleAddLease() {
        Dialog<Lease> dialog = new Dialog<>();
        dialog.setTitle("Add New Lease Agreement");
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20, 50, 10, 10));

        TextField tenantNameField = new TextField(); tenantNameField.setPromptText("Full Name");
        TextField roomNumField = new TextField(); roomNumField.setPromptText("Room Number");
        DatePicker startDatePicker = new DatePicker(); DatePicker endDatePicker = new DatePicker();

        grid.add(new Label("Tenant Name:"), 0, 0); grid.add(tenantNameField, 1, 0);
        grid.add(new Label("Room Number:"), 0, 1); grid.add(roomNumField, 1, 1);
        grid.add(new Label("Start Date:"), 0, 2); grid.add(startDatePicker, 1, 2);
        grid.add(new Label("End Date:"), 0, 3); grid.add(endDatePicker, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (startDatePicker.getValue() == null || endDatePicker.getValue() == null || tenantNameField.getText().isEmpty()) {
                    new Alert(Alert.AlertType.ERROR, "Please fill out all fields.").show();
                    return null;
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                int fakeId = 1000 + leaseList.size() + 1;
                return new Lease(fakeId, tenantNameField.getText(), roomNumField.getText(),
                        startDatePicker.getValue().format(formatter),
                        endDatePicker.getValue().format(formatter), "Pending");
            }
            return null;
        });

        Optional<Lease> result = dialog.showAndWait();
        result.ifPresent(lease -> leaseList.add(lease));
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