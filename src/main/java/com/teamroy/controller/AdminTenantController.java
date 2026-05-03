package com.teamroy.controller;

import com.teamroy.CurrencyUtil;

import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.ImportExportService;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class AdminTenantController {

    private static final DateTimeFormatter MDFMT = DateTimeFormatter.ofPattern("MM/dd");

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Room> roomFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private TableView<Tenant> tenantTable;
    @FXML
    private TableColumn<Tenant, String> colAvatar;
    @FXML
    private TableColumn<Tenant, String> colName;
    @FXML
    private TableColumn<Tenant, String> colRoom;
    @FXML
    private TableColumn<Tenant, String> colLeaseStart;
    @FXML
    private TableColumn<Tenant, String> colLeaseEnd;
    @FXML
    private TableColumn<Tenant, String> colRentStatus;
    @FXML
    private TableColumn<Tenant, String> colReqCount;
    @FXML
    private Label detailInitials;
    @FXML
    private Label detailName;
    @FXML
    private Label detailEmail;
    @FXML
    private Label detailContact;
    @FXML
    private Label detailAddedDate;
    @FXML
    private Label detailBalance;
    @FXML
    private Label detailRentStatus;
    @FXML
    private Label detailNextDue;
    @FXML
    private Label detailRoomNumber;
    @FXML
    private Label detailRoomType;
    @FXML
    private Label detailLeaseStatus;
    @FXML
    private ToggleButton archivedToggle;
    @FXML
    private TableView<Tenant> archivedTable;
    @FXML
    private TableColumn<Tenant, String> archColName;
    @FXML
    private TableColumn<Tenant, String> archColEmail;
    @FXML
    private TableColumn<Tenant, Void> archActions;

    private Connection conn;
    private TenantDaoImpl tenantDao;
    private LeaseDaoImpl leaseDao;
    private RoomDaoImpl roomDao;
    private MaintenanceRequestDaoImpl maintenanceDao;
    private final ImportExportService importExportService = new ImportExportService();

    private PauseTransition searchPause;

    private Connection openConnection() throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        }
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }

    private Room sentinelAllRooms() {
        Room r = new Room();
        r.SetRoomID(-1);
        r.SetRoomNumber("All rooms");
        return r;
    }

    @FXML
    private void initialize() {
        try {
            conn = openConnection();
            tenantDao = new TenantDaoImpl(conn);
            leaseDao = new LeaseDaoImpl(conn);
            roomDao = new RoomDaoImpl(conn);
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        roomFilterCombo.getItems().add(sentinelAllRooms());
        roomFilterCombo.getItems().addAll(roomDao.GetAll());
        roomFilterCombo.getSelectionModel().selectFirst();
        bindRoomCells();

        statusFilterCombo.setItems(FXCollections.observableArrayList("ALL", "PAID", "UNPAID"));
        statusFilterCombo.getSelectionModel().selectFirst();

        configureTenantColumns();
        configureArchivedColumns();

        searchPause = new PauseTransition(Duration.millis(300));
        searchPause.setOnFinished(ev -> reloadMainTable());

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchPause.stop();
            searchPause.playFromStart();
        });

        roomFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> reloadMainTable());
        statusFilterCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> reloadMainTable());

        tenantTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> onTenantSelected(n));

        archivedToggle.selectedProperty().addListener((obs, o, shown) -> {
            archivedTable.setVisible(shown);
            archivedTable.setManaged(shown);
            if (Boolean.TRUE.equals(shown)) {
                reloadArchivedTable();
            }
        });

        reloadMainTable();
    }

    private void bindRoomCells() {
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

    private void configureTenantColumns() {
        colAvatar.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(initials(cd.getValue())));
        colName.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().GetFirstName() + " " + cd.getValue().GetLastName()));
        colRoom.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(roomLabel(cd.getValue())));
        colLeaseStart.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(leaseStart(cd.getValue())));
        colLeaseEnd.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(leaseEnd(cd.getValue())));
        colRentStatus.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(rentStatus(cd.getValue())));
        colReqCount.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(String.valueOf(maintenanceDao.GetByTenantID(cd.getValue().GetTenantID()).size())));
    }

    private void configureArchivedColumns() {
        archColName.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(cd.getValue().GetFirstName() + " " + cd.getValue().GetLastName()));
        archColEmail.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(
                cd.getValue().GetEmail() == null ? "" : cd.getValue().GetEmail()));

        archActions.setCellFactory(col -> new TableCell<Tenant, Void>() {
            private final Button btn = new Button("Restore");

            {
                btn.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold;");
                btn.setOnAction(evt -> {
                    Tenant row = getTableRow().getItem();
                    if (row == null) {
                        return;
                    }
                    tenantDao.Restore(row.GetTenantID());
                    reloadArchivedTable();
                    reloadMainTable();
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btn);
                }
            }
        });
    }

    private Lease pickLease(Tenant tenant) {
        List<Lease> leases = leaseDao.GetByTenantId(tenant.GetTenantID());
        Optional<Lease> active = leases.stream().filter(l -> "ACTIVE".equalsIgnoreCase(l.GetStatus())).findFirst();
        return active.orElseGet(() -> leases.isEmpty() ? null : leases.get(0));
    }

    private String roomLabel(Tenant tenant) {
        Lease lease = pickLease(tenant);
        if (lease == null) {
            return "—";
        }
        Room room = roomDao.GetByID(lease.GetRoomID());
        return room == null ? ("#" + lease.GetRoomID()) : room.GetRoomNumber();
    }

    private String leaseStart(Tenant tenant) {
        Lease lease = pickLease(tenant);
        return lease == null ? "—" : lease.GetStartDate().toString();
    }

    private String leaseEnd(Tenant tenant) {
        Lease lease = pickLease(tenant);
        return lease == null ? "—" : lease.GetEndDate().toString();
    }

    private String rentStatus(Tenant tenant) {
        return tenant.GetTotalBalance() <= 0.01 ? "Paid" : "Unpaid";
    }

    private String initials(Tenant tenant) {
        String f = tenant.GetFirstName() == null ? "" : tenant.GetFirstName();
        String l = tenant.GetLastName() == null ? "" : tenant.GetLastName();
        String fi = f.isBlank() ? "" : f.substring(0, 1).toUpperCase();
        String li = l.isBlank() ? "" : l.substring(0, 1).toUpperCase();
        String combo = fi + li;
        return combo.isBlank() ? "?" : combo;
    }

    private boolean roomMatches(Tenant tenant) {
        Room roomSel = roomFilterCombo.getSelectionModel().getSelectedItem();
        if (roomSel == null || roomSel.GetRoomID() < 0) {
            return true;
        }
        Lease lease = pickLease(tenant);
        return lease != null && lease.GetRoomID() == roomSel.GetRoomID();
    }

    private boolean statusMatches(Tenant tenant) {
        String st = statusFilterCombo.getSelectionModel().getSelectedItem();
        if (st == null || "ALL".equalsIgnoreCase(st)) {
            return true;
        }
        boolean paid = tenant.GetTotalBalance() <= 0.01;
        if ("PAID".equalsIgnoreCase(st)) {
            return paid;
        }
        if ("UNPAID".equalsIgnoreCase(st)) {
            return !paid;
        }
        return true;
    }

    private void reloadMainTable() {
        List<Tenant> base;
        String q = searchField.getText() == null ? "" : searchField.getText().trim();
        if (q.isBlank()) {
            base = tenantDao.GetAllActive();
        } else {
            base = tenantDao.GetByName(q);
            base = base.stream().filter(t -> t.GetTimeDeletedAt() == null).collect(Collectors.toList());
        }

        List<Tenant> filtered = base.stream()
                .filter(this::roomMatches)
                .filter(this::statusMatches)
                .collect(Collectors.toList());
        ObservableList<Tenant> items = FXCollections.observableArrayList(filtered);
        tenantTable.setItems(items);
        tenantTable.refresh();

        Tenant selected = tenantTable.getSelectionModel().getSelectedItem();
        if (selected != null && !filtered.contains(selected)) {
            tenantTable.getSelectionModel().clearSelection();
            onTenantSelected(null);
        }
    }

    private void reloadArchivedTable() {
        List<Tenant> archived = tenantDao.GetAll().stream()
                .filter(t -> t.GetTimeDeletedAt() != null)
                .collect(Collectors.toList());
        archivedTable.setItems(FXCollections.observableArrayList(archived));
    }

    private void onTenantSelected(Tenant tenant) {
        if (tenant == null) {
            detailName.setText("—");
            detailEmail.setText("");
            detailContact.setText("");
            detailAddedDate.setText("Added: —");
            detailInitials.setText("TN");
            detailBalance.setText("Total Balance: —");
            detailRentStatus.setText("Rent status: —");
            detailNextDue.setText("Next due: —");
            detailRoomNumber.setText("Room: —");
            detailRoomType.setText("Type: —");
            detailLeaseStatus.setText("Lease status: —");
            return;
        }

        detailInitials.setText(initials(tenant));
        detailName.setText(tenant.GetFirstName() + " " + tenant.GetLastName());
        detailEmail.setText(tenant.GetEmail());
        detailContact.setText(tenant.GetContactNumber());
        detailAddedDate.setText("Added: —");

        detailBalance.setText("Total Balance: " + CurrencyUtil.format(tenant.GetTotalBalance()));
        detailRentStatus.setText("Rent status: " + rentStatus(tenant));

        Lease lease = pickLease(tenant);
        if (lease == null) {
            detailNextDue.setText("Next due: —");
            detailRoomNumber.setText("Room: —");
            detailRoomType.setText("Type: —");
            detailLeaseStatus.setText("Lease status: —");
            return;
        }

        Room room = roomDao.GetByID(lease.GetRoomID());
        detailLeaseStatus.setText("Lease status: " + lease.GetStatus());
        detailRoomNumber.setText("Room: " + (room == null ? ("#" + lease.GetRoomID()) : room.GetRoomNumber()));
        detailRoomType.setText("Type: " + (room == null ? "—" : room.GetRoomType()));

        LocalDate nextDue = lease.GetStartDate().plusMonths(1);
        detailNextDue.setText("Next due: " + MDFMT.format(nextDue));

        tenantTable.refresh();
    }

    @FXML
    private void handleAddTenant() {
        dialogTenant(null);
    }

    @FXML
    private void handleEditTenant() {
        Tenant t = tenantTable.getSelectionModel().getSelectedItem();
        if (t == null) {
            return;
        }
        dialogTenant(t);
    }

    private void dialogTenant(Tenant edit) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_tenant_dialog.fxml"));
            Parent root = loader.load();
            AddTenantDialogController c = loader.getController();
            if (edit == null) {
                c.configureCreate(conn, () -> {
                    reloadMainTable();
                    reloadArchivedTable();
                });
            } else {
                c.configureEdit(conn, edit, () -> {
                    reloadMainTable();
                    onTenantSelected(edit);
                });
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle(edit == null ? "Add tenant" : "Edit tenant");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            reloadMainTable();
            if (Boolean.TRUE.equals(archivedToggle.isSelected())) {
                reloadArchivedTable();
            }

            Tenant current = tenantTable.getSelectionModel().getSelectedItem();
            if (current != null) {
                Tenant refreshed = tenantDao.GetByID(current.GetTenantID());
                onTenantSelected(refreshed);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleRemoveTenant() {
        Tenant t = tenantTable.getSelectionModel().getSelectedItem();
        if (t == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove tenant");
        alert.setHeaderText("Soft delete this tenant?");
        alert.setContentText(t.GetFirstName() + " " + t.GetLastName());
        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isPresent() && choice.get() == ButtonType.OK) {
            tenantDao.Delete(t.GetTenantID());
            tenantTable.getSelectionModel().clearSelection();
            reloadMainTable();
            if (Boolean.TRUE.equals(archivedToggle.isSelected())) {
                reloadArchivedTable();
            }
            onTenantSelected(null);
        }
    }

    @FXML
    private void handleExportCsv() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Export tenants CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            Stage stage = (Stage) tenantTable.getScene().getWindow();
            File dest = chooser.showSaveDialog(stage);
            if (dest == null) {
                return;
            }
            importExportService.exportTenantsToCSV(tenantDao.GetAllActive(), dest.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void handleImportCsv() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import tenants CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            Stage stage = (Stage) tenantTable.getScene().getWindow();
            File src = chooser.showOpenDialog(stage);
            if (src == null) {
                return;
            }
            int[] res = importExportService.importTenantsFromCSV(src.getAbsolutePath(), tenantDao);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Import complete");
            alert.setHeaderText(null);
            alert.setContentText("Imported: " + res[0] + " tenants. Skipped rows: " + res[1] + ".");
            alert.showAndWait();
            reloadMainTable();
            if (Boolean.TRUE.equals(archivedToggle.isSelected())) {
                reloadArchivedTable();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
