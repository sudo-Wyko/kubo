package com.teamroy.controller;

import com.teamroy.DatabaseUtility;
import com.teamroy.model.dao.RoomDao;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.dao.LeaseDao;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Lease;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class TenantRoomsController {

    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private FlowPane roomGrid;

    private Connection conn = DatabaseUtility.getConnection();
    private RoomDaoImpl roomDao = new RoomDaoImpl(conn);
    private LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);

    @FXML
    public void initialize() {
        statusFilter.getItems().addAll("All", "Available", "Full");
        statusFilter.setValue("All");
        statusFilter.getStyleClass().add("payments-filter");

        statusFilter.valueProperty().addListener((observable, oldValue, newValue) -> {
            loadRooms();
        });

        loadRooms();
    }

    private void loadRooms() {
        roomGrid.getChildren().clear();

        String currentFilter = statusFilter.getValue();
        List<Room> rooms = roomDao.GetAll();

        for (Room room : rooms) {
            List<Lease> activeLeases = leaseDao.GetActiveLeasesByRoom(room.GetRoomID());
            int actualOccupancy = activeLeases.size();
            boolean isAvailable = actualOccupancy < room.GetCapacity();
            String status = isAvailable ? "Available" : "Full";

            if ("Available".equals(currentFilter) && !isAvailable)
                continue;
            if ("Full".equals(currentFilter) && isAvailable)
                continue;

            String roomNum = room.GetRoomNumber();
            String type = room.GetRoomType();
            String price = String.format("%,.2f", room.GetPrice());
            String residents = "Occupied (" + actualOccupancy + "/" + room.GetCapacity() + ")";

            VBox card = createRoomCard(roomNum, type, status, price, residents);
            roomGrid.getChildren().add(card);
        }
    }

    private VBox createRoomCard(String roomNum, String type, String status, String price, String residents) {
        VBox card = new VBox();
        card.getStyleClass().add("room-card");
        card.setPrefWidth(260);
        card.setPrefHeight(320);

        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setPrefHeight(150);
        imagePlaceholder.getStyleClass().add("room-card-image");

        VBox details = new VBox(8);
        details.setPadding(new Insets(15));

        Label nameLabel = new Label("Room " + roomNum);
        nameLabel.getStyleClass().add("room-card-title");

        HBox typeStatusBox = new HBox(10);
        typeStatusBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("card-subtitle");

        Label statusBadge = new Label(status);
        statusBadge.getStyleClass().add("status-badge");
        statusBadge.getStyleClass().add(
                status.equals("Available") ? "room-status-available" : "room-status-full");

        typeStatusBox.getChildren().addAll(typeLabel, statusBadge);

        Label priceLabel = new Label("\u20b1" + price + "/month");
        priceLabel.getStyleClass().add("room-card-price");
        VBox.setMargin(priceLabel, new Insets(10, 0, 5, 0));

        HBox residentBox = new HBox(8);
        residentBox.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("\uD83D\uDC64");
        icon.getStyleClass().add("card-subtitle");

        Label residentsLabel = new Label(residents);
        residentsLabel.getStyleClass().add("card-subtitle");

        residentBox.getChildren().addAll(icon, residentsLabel);

        details.getChildren().addAll(nameLabel, typeStatusBox, priceLabel, residentBox);
        card.getChildren().addAll(imagePlaceholder, details);

        return card;
    }
}