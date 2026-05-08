package com.teamroy.controller;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.TenantDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import com.teamroy.service.LeaseService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.time.LocalDate;
public class AddLeaseDialogController {
    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Tenant> tenantCombo;
    @FXML
    private ComboBox<Room> roomCombo;
    @FXML
    private DatePicker startPicker;
    @FXML
    private DatePicker endPicker;
    @FXML
    private TextField rentField;
    @FXML
    private Label dateErrorLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;
    private LeaseDaoImpl leaseDao;
    private LeaseService leaseService;
    private Runnable onSuccess;
    private Lease editingLease;
    @FXML
    private void initialize() {
        wireDateValidation();
    }
    public void configureCreate(Connection conn, Runnable onSuccessCallback) {
        this.leaseDao = new LeaseDaoImpl(conn);
        this.leaseService = new LeaseService(conn);
        this.onSuccess = onSuccessCallback;
        this.editingLease = null;
        TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
        RoomDaoImpl roomDao = new RoomDaoImpl(conn);
        tenantCombo.setDisable(false);
        tenantCombo.setItems(FXCollections.observableArrayList(tenantDao.GetAllActive()));
        roomCombo.setItems(FXCollections.observableArrayList(roomDao.GetAll()));
        tenantCombo.setButtonCell(createTenantCell());
        tenantCombo.setCellFactory(lv -> createTenantCell());
        roomCombo.setButtonCell(createRoomCell());
        roomCombo.setCellFactory(lv -> createRoomCell());
        tenantCombo.getSelectionModel().clearSelection();
        roomCombo.getSelectionModel().clearSelection();
        titleLabel.setText("Create lease");
        startPicker.setValue(null);
        endPicker.setValue(null);
        rentField.clear();
        clearInlineErrors();
    }
    public void configureEdit(Connection conn, Lease leaseToEdit, Runnable onSuccessCallback) {
        configureCreate(conn, onSuccessCallback);
        this.editingLease = leaseToEdit;
        TenantDaoImpl tenantDao = new TenantDaoImpl(conn);
        Tenant matchTenant = tenantDao.GetByID(leaseToEdit.GetTenantID());
        if (matchTenant != null) {
            tenantCombo.getSelectionModel().select(matchTenant);
        }
        Room matchRoom = new RoomDaoImpl(conn).GetByID(leaseToEdit.GetRoomID());
        if (matchRoom != null) {
            roomCombo.getSelectionModel().select(matchRoom);
        }
        tenantCombo.setDisable(true);
        startPicker.setValue(leaseToEdit.GetStartDate());
        endPicker.setValue(leaseToEdit.GetEndDate());
        rentField.setText(Double.toString(leaseToEdit.GetMonthlyRent()));
        titleLabel.setText("Edit lease");
    }
    private void wireDateValidation() {
        startPicker.valueProperty().addListener((o, ov, nv) -> validateDatesInline());
        endPicker.valueProperty().addListener((o, ov, nv) -> validateDatesInline());
    }
    private ListCell<Tenant> createTenantCell() {
        return new ListCell<Tenant>() {
            @Override
            protected void updateItem(Tenant item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.GetFirstName() + " " + item.GetLastName());
                }
            }
        };
    }
    private ListCell<Room> createRoomCell() {
        return new ListCell<Room>() {
            @Override
            protected void updateItem(Room item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.GetRoomNumber() + " â€¢ " + item.GetRoomType());
                }
            }
        };
    }
    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }
    private void clearInlineErrors() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        dateErrorLabel.setManaged(false);
        dateErrorLabel.setVisible(false);
    }
    private boolean validateDatesInline() {
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();
        if (start != null && end != null && !end.isAfter(start)) {
            dateErrorLabel.setText("End date must be after start date.");
            dateErrorLabel.setManaged(true);
            dateErrorLabel.setVisible(true);
            return false;
        }
        dateErrorLabel.setManaged(false);
        dateErrorLabel.setVisible(false);
        return true;
    }
    @FXML
    private void save() {
        clearInlineErrors();
        Tenant tenant = tenantCombo.getSelectionModel().getSelectedItem();
        Room room = roomCombo.getSelectionModel().getSelectedItem();
        LocalDate start = startPicker.getValue();
        LocalDate end = endPicker.getValue();
        if (tenant == null || room == null || start == null || end == null) {
            showError("Tenant, room, start, and end dates are required.");
            return;
        }
        if (!validateDatesInline()) {
            return;
        }
        double rent;
        try {
            rent = Double.parseDouble(rentField.getText().trim());
            if (!(rent > 0)) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showError("Monthly rent must be greater than zero.");
            return;
        }
        if (editingLease == null) {
            Lease created =
                    leaseService.createLease(tenant.GetTenantID(), room.GetRoomID(), start, end, rent);
            if (created == null) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lease unavailable");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Room is not available for the selected date range. Please choose a different room or dates.");
                alert.showAndWait();
                return;
            }
            notifySuccessClose();
            return;
        }
        Integer excludeId = editingLease.GetLeaseID();
        if (!leaseDao.IsRoomAvailable(room.GetRoomID(), start, end, excludeId)) {
            showError("Selected room overlaps another active lease for those dates.");
            return;
        }
        editingLease.SetRoomID(room.GetRoomID());
        editingLease.SetStartDate(start);
        editingLease.SetEndDate(end);
        editingLease.SetMonthlyRent(rent);
        leaseDao.Update(editingLease);
        notifySuccessClose();
    }
    private void notifySuccessClose() {
        if (onSuccess != null) {
            onSuccess.run();
        }
        cancel();
    }
    @FXML
    private void cancel() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
