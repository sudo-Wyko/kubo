package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.ConnectionManager;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Room;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class AdminRoomController {

    public static final class RoomRow {

        private final Room room;
        private final String status;
        private final String occupancySummary;
        private final String maintInfo;

        public RoomRow(Room room, String status, String occupancySummary, String maintInfo) {
            this.room = room;
            this.status = status;
            this.occupancySummary = occupancySummary;
            this.maintInfo = maintInfo;
        }

        public Room getRoom() {
            return room;
        }

        public String getStatus() {
            return status;
        }

        public String getOccupancySummary() {
            return occupancySummary;
        }

        public String getMaintInfo() {
            return maintInfo;
        }
    }

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private TableView<RoomRow> roomTable;
    @FXML
    private TableColumn<RoomRow, Integer> colId;
    @FXML
    private TableColumn<RoomRow, String> colNumber;
    @FXML
    private TableColumn<RoomRow, String> colType;
    @FXML
    private TableColumn<RoomRow, String> colCapacity;
    @FXML
    private TableColumn<RoomRow, String> colOccupancy;
    @FXML
    private TableColumn<RoomRow, String> colPrice;
    @FXML
    private TableColumn<RoomRow, String> colStatus;
    @FXML
    private TableColumn<RoomRow, String> colMaintenance;
    @FXML
    private TableColumn<RoomRow, Void> colActions;

    private Connection conn;
    private RoomDaoImpl roomDao;
    private MaintenanceRequestDaoImpl maintenanceDao;

    private List<Room> cachedRooms = List.of();

    private final PauseTransition searchDebounce = new PauseTransition(Duration.millis(300));

    @FXML
    private void initialize() {
        try {
            conn = ConnectionManager.getConnection();
            roomDao = new RoomDaoImpl(conn);
            maintenanceDao = new MaintenanceRequestDaoImpl(conn);
        } catch (Exception ex) {
            System.err.println("Failed to initialize rooms view: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        typeFilterCombo.setItems(FXCollections.observableArrayList(
                "All", "Single", "Double", "Dormitory"));
        typeFilterCombo.getSelectionModel().selectFirst();

        statusFilterCombo.setItems(FXCollections.observableArrayList(
                "All", "Available", "Occupied", "Maintenance"));
        statusFilterCombo.getSelectionModel().selectFirst();

        typeFilterCombo.getSelectionModel().selectedItemProperty()
                .addListener((o, ov, nv) -> applyFiltersRebuild());
        statusFilterCombo.getSelectionModel().selectedItemProperty()
                .addListener((o, ov, nv) -> applyFiltersRebuild());

        searchDebounce.setOnFinished(e -> applyFiltersRebuild());
        searchField.textProperty().addListener((o, ov, nv) -> searchDebounce.playFromStart());

        configureColumns();
        reloadRooms();
    }

    private void configureColumns() {
        colId.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getRoom().GetRoomID()));

        colNumber.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getRoom().GetRoomNumber()));

        colType.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getRoom().GetRoomType()));

        colCapacity.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(Integer.toString(cd.getValue().getRoom().GetCapacity())));

        colOccupancy.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getOccupancySummary()));

        colPrice.setCellValueFactory(cd ->
                new ReadOnlyObjectWrapper<>(CurrencyUtil.format(cd.getValue().getRoom().GetPrice())));

        colStatus.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getStatus()));

        colMaintenance.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue().getMaintInfo()));

        colMaintenance.setCellFactory(col -> new TableCell<RoomRow, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setWrapText(true);
                    setPrefWidth(USE_COMPUTED_SIZE);
                    setMaxWidth(Double.MAX_VALUE);
                    setText(item);
                }
            }
        });

        colActions.setCellFactory(col -> new TableCell<RoomRow, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                Room r = getTableRow().getItem().getRoom();
                Button edit = new Button("Edit");
                edit.setStyle("-fx-background-color: #374151; -fx-text-fill: white;");
                edit.setOnAction(ev -> handleEdit(r));

                Button del = new Button("Delete");
                del.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: white;");
                del.setOnAction(ev -> handleDelete(r));

                HBox box = new HBox(6, edit, del);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setPadding(new Insets(2));
                setGraphic(box);
            }
        });
    }

    @FXML
    private void handleAddRoom() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_room_dialog.fxml"));
            Parent root = loader.load();
            AddRoomDialogController c = loader.getController();
            c.configureCreate(conn, this::reloadRooms);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add room");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleEdit(Room room) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/teamroy/add_room_dialog.fxml"));
            Parent root = loader.load();
            AddRoomDialogController c = loader.getController();
            c.configureEdit(conn, room, this::reloadRooms);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Edit room");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleDelete(Room room) {
        if (room.GetCurrentOccupancy() > 0) {
            Alert a = new Alert(Alert.AlertType.WARNING);
            a.setTitle("Cannot delete room");
            a.setHeaderText(null);
            a.setContentText("Cannot delete an occupied room.");
            a.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete room");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete room " + room.GetRoomNumber() + "?");
        confirm.getButtonTypes().setAll(
                new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE), ButtonType.CANCEL);
        confirm.showAndWait().ifPresent(answer -> {
            if (answer.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                roomDao.Delete(room.GetRoomID());
                reloadRooms();
            }
        });
    }

    private void reloadRooms() {
        cachedRooms = roomDao.GetAll();
        applyFiltersRebuild();
    }

    private RoomRow rowForRoom(Room r) {
        List<MaintenanceRequest> list = maintenanceDao.GetByRoomID(r.GetRoomID());
        boolean hasOpenMaintenance = list.stream().anyMatch(openMaintPredicate());

        String maintSumm = summarizeOpenMaintenance(list);
        String occupancy = r.GetCurrentOccupancy() + " / " + r.GetCapacity();

        String st;
        if (r.GetCurrentOccupancy() >= r.GetCapacity()) {
            st = "Occupied";
        } else if (hasOpenMaintenance) {
            st = "Maintenance";
        } else {
            st = "Available";
        }

        return new RoomRow(r, st, occupancy, maintSumm);
    }

    private Predicate<MaintenanceRequest> openMaintPredicate() {
        return m -> "NEW".equalsIgnoreCase(m.GetStatus())
                || "IN-PROGRESS".equalsIgnoreCase(m.GetStatus())
                || "IN_PROGRESS".equalsIgnoreCase(m.GetStatus());
    }

    private String summarizeOpenMaintenance(List<MaintenanceRequest> list) {
        return list.stream().filter(openMaintPredicate())
                .max(Comparator.comparing(MaintenanceRequest::GetReportedDate,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(d -> shorten(d.GetReportDescription(), 120))
                .orElse("None");
    }

    private static String shorten(String s, int max) {
        if (s == null) {
            return "None";
        }
        String t = s.replace('\n', ' ').trim();
        if (t.isEmpty()) {
            return "None";
        }
        return t.length() > max ? t.substring(0, max - 3) + "..." : t;
    }

    private void applyFiltersRebuild() {
        String qRaw = searchField.getText().trim().toLowerCase(Locale.ROOT);
        String typePick = typeFilterCombo.getSelectionModel().getSelectedItem();
        String statusPick = statusFilterCombo.getSelectionModel().getSelectedItem();

        List<RoomRow> rows =
                cachedRooms.stream()
                        .map(this::rowForRoom)
                        .filter(row -> bySearch(row.getRoom(), qRaw))
                        .filter(row -> byType(row.getRoom(), typePick))
                        .filter(row -> byStatus(row, statusPick))
                        .collect(Collectors.toList());

        roomTable.setItems(FXCollections.observableArrayList(rows));
    }

    private boolean bySearch(Room r, String qRaw) {
        if (qRaw.isEmpty()) {
            return true;
        }
        return r.GetRoomNumber() != null && r.GetRoomNumber().toLowerCase(Locale.ROOT).contains(qRaw);
    }

    private boolean byType(Room r, String typePick) {
        if ("All".equalsIgnoreCase(typePick)) {
            return true;
        }
        return r.GetRoomType() != null && r.GetRoomType().equalsIgnoreCase(typePick.trim());
    }

    private boolean byStatus(RoomRow row, String statusPick) {
        if ("All".equalsIgnoreCase(statusPick)) {
            return true;
        }
        return row.getStatus().equalsIgnoreCase(statusPick);
    }
}
