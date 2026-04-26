package com.teamroy.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

public class TenantRoomsController {

    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private FlowPane roomGrid;

    @FXML
    public void initialize() {
        // Setup the filter dropdown
        statusFilter.getItems().addAll("All", "Available", "Full");
        statusFilter.setValue("All");

        // Load the initial rooms
        loadRooms();
    }

    private void loadRooms() {
        roomGrid.getChildren().clear();

        // TODO: Replace with DAO!
        // List<Room> rooms = roomDao.getAllRooms();
        // for(Room room : rooms) { roomGrid.getChildren().add(createRoomCard(...)); }

        // dummy data for testing
        roomGrid.getChildren().add(createRoomCard("101", "Double bunk", "Available", "4,500", "You, Jane Doe"));
        roomGrid.getChildren().add(createRoomCard("102", "Single bunk", "Full", "4,500", "Xanti, Ken"));
        roomGrid.getChildren().add(createRoomCard("103", "Single bed", "Full", "4,500", "Mama"));
    }

    private VBox createRoomCard(String roomNum, String type, String status, String price, String residents) {

        VBox card = new VBox();
        card.setStyle(
                "-fx-background-color: #1a1a1a; -fx-border-radius: 15; -fx-background-radius: 15; -fx-border-color: white; -fx-border-width: 1.5;");
        card.setPrefWidth(260);
        card.setPrefHeight(320);

        StackPane imagePlaceholder = new StackPane();
        imagePlaceholder.setPrefHeight(150);
        imagePlaceholder.setStyle(
                "-fx-background-color: #333333; -fx-background-radius: 13 13 0 0; -fx-border-color: white; -fx-border-width: 0 0 1.5 0;");

        VBox details = new VBox(8);
        details.setPadding(new Insets(15));

        Label nameLabel = new Label("Room " + roomNum);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold;");

        HBox typeStatusBox = new HBox(10);
        typeStatusBox.setAlignment(Pos.CENTER_LEFT);

        Label typeLabel = new Label(type);
        typeLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 14px;");

        Label statusBadge = new Label(status);
        String badgeColor = status.equals("Available") ? "#a3e635" : "#f87171";
        statusBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: " + badgeColor + "; -fx-border-color: "
                + badgeColor + "; -fx-border-radius: 4; -fx-padding: 2 8 2 8; -fx-font-size: 12px;");

        typeStatusBox.getChildren().addAll(typeLabel, statusBadge);

        Label priceLabel = new Label("$" + price + "/month");
        priceLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold;");
        VBox.setMargin(priceLabel, new Insets(10, 0, 5, 0)); // Add some vertical spacing

        HBox residentBox = new HBox(8);
        residentBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("\uD83D\uDC64"); // Unicode for a user icon
        icon.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        Label residentsLabel = new Label(residents);
        residentsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px;");
        residentBox.getChildren().addAll(icon, residentsLabel);

        details.getChildren().addAll(nameLabel, typeStatusBox, priceLabel, residentBox);
        card.getChildren().addAll(imagePlaceholder, details);

        return card;
    }
}