package com.teamroy.controller;

import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.entity.Room;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.stage.Stage;

import java.sql.Connection;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class AddRoomDialogController {

    private static final Pattern CAPACITY_DIGITS = Pattern.compile("\\d*");
    private static final Pattern PRICE_NUMERIC = Pattern.compile("\\d*(\\.\\d{0,2})?");

    @FXML
    private Label titleLabel;
    @FXML
    private TextField roomNumberField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField priceField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    private RoomDaoImpl roomDao;
    private Runnable onSuccess;
    /** Non-null → edit existing room */
    private Room editing;

    @FXML
    private void initialize() {
        UnaryOperator<TextFormatter.Change> capFilter = change -> {
            String t = change.getControlNewText();
            return CAPACITY_DIGITS.matcher(t).matches() ? change : null;
        };
        capacityField.setTextFormatter(new TextFormatter<>(capFilter));

        UnaryOperator<TextFormatter.Change> priceFilter = change -> {
            String t = change.getControlNewText();
            return PRICE_NUMERIC.matcher(t).matches() ? change : null;
        };
        priceField.setTextFormatter(new TextFormatter<>(priceFilter));
    }

    public void configureCreate(Connection conn, Runnable onSuccessCallback) {
        this.roomDao = new RoomDaoImpl(conn);
        this.onSuccess = onSuccessCallback;
        this.editing = null;

        titleLabel.setText("Add room");
        typeCombo.setItems(FXCollections.observableArrayList("Single", "Double", "Dormitory"));
        typeCombo.getSelectionModel().selectFirst();

        roomNumberField.clear();
        capacityField.clear();
        priceField.clear();
        clearError();
    }

    public void configureEdit(Connection conn, Room room, Runnable onSuccessCallback) {
        configureCreate(conn, onSuccessCallback);
        this.editing = room;

        titleLabel.setText("Edit room");
        roomNumberField.setText(room.GetRoomNumber());
        selectType(room.GetRoomType());
        capacityField.setText(Integer.toString(room.GetCapacity()));
        priceField.setText(Double.toString(room.GetPrice()));
    }

    private void selectType(String dbType) {
        if (dbType == null) {
            typeCombo.getSelectionModel().selectFirst();
            return;
        }
        for (String choice : typeCombo.getItems()) {
            if (choice.equalsIgnoreCase(dbType.trim())) {
                typeCombo.getSelectionModel().select(choice);
                return;
            }
        }
        typeCombo.getSelectionModel().selectFirst();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
    }

    @FXML
    private void save() {
        clearError();

        String number = trim(roomNumberField.getText());
        if (number.isBlank()) {
            showError("Room number is required.");
            return;
        }

        String type = typeCombo.getSelectionModel().getSelectedItem();
        if (type == null || type.isBlank()) {
            showError("Select a room type.");
            return;
        }

        int capacity;
        try {
            capacity = Integer.parseInt(trim(capacityField.getText()));
            if (capacity < 1) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showError("Capacity must be at least 1.");
            return;
        }

        double price;
        try {
            String pText = trim(priceField.getText());
            if (pText.isBlank()) {
                price = 0;
            } else {
                price = Double.parseDouble(pText);
            }
            if (price < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            showError("Enter a valid price (0 or greater).");
            return;
        }

        Room existing = roomDao.GetByRoomNumber(number);
        if (existing != null && (editing == null || existing.GetRoomID() != editing.GetRoomID())) {
            showError("Room number is already in use.");
            return;
        }

        if (editing == null) {
            Room room = new Room();
            room.SetRoomNumber(number);
            room.SetRoomType(type);
            room.SetCapacity(capacity);
            room.SetCurrentOccupancy(0);
            room.SetPrice(price);
            roomDao.Create(room);
        } else {
            editing.SetRoomNumber(number);
            editing.SetRoomType(type);
            editing.SetCapacity(capacity);
            roomDao.Update(editing);
            roomDao.UpdatePrice(editing.GetRoomID(), price);
        }

        if (onSuccess != null) {
            onSuccess.run();
        }
        cancel();
    }

    private static String trim(String text) {
        return text == null ? "" : text.trim();
    }

    @FXML
    private void cancel() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
