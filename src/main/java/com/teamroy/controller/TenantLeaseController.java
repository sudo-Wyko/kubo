package com.teamroy.controller;

import com.teamroy.ConnectionManager;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.Room;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.util.Comparator;
import java.util.List;

public class TenantLeaseController {
    @FXML
    private VBox leaseListContainer;
    @FXML
    private Label headerLabel;
    @FXML
    private Label emptyStateLabel;

    private final Connection conn = ConnectionManager.getConnection();
    private final LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
    private final RoomDaoImpl roomDao = new RoomDaoImpl(conn);

    @FXML
    public void initialize() {
        loadLeaseData(SessionManager.getCurrentTenantId());
    }

    private void loadLeaseData(int tenantId) {
        leaseListContainer.getChildren().setAll(headerLabel, emptyStateLabel);

        List<Lease> leases = leaseDao.GetByTenantId(tenantId);
        leases.sort(Comparator.comparing(Lease::GetStartDate, Comparator.nullsLast(Comparator.reverseOrder())));

        if (leases.isEmpty()) {
            emptyStateLabel.setVisible(true);
            emptyStateLabel.setManaged(true);
            return;
        }

        emptyStateLabel.setVisible(false);
        emptyStateLabel.setManaged(false);

        for (Lease lease : leases) {
            leaseListContainer.getChildren().add(buildLeaseCard(lease));
        }
    }

    private VBox buildLeaseCard(Lease lease) {
        Room room = roomDao.GetByID(lease.GetRoomID());
        String roomLabel = room == null ? ("Room #" + lease.GetRoomID()) : room.GetRoomNumber();

        Label cardTitle = new Label("Room " + roomLabel + " • Lease #" + lease.GetLeaseID());
        cardTitle.getStyleClass().add("card-title");

        HBox body = new HBox(20);
        body.getChildren().add(buildDocumentPane());
        body.getChildren().add(buildStatsColumn(lease));

        VBox card = new VBox(12, cardTitle, body);
        card.getStyleClass().addAll("card", "lease-card");
        card.setPadding(new Insets(16));
        return card;
    }

    private StackPane buildDocumentPane() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("lease-document-pane");
        pane.setMinHeight(280);
        HBox.setHgrow(pane, javafx.scene.layout.Priority.ALWAYS);

        VBox content = new VBox(8);
        content.setAlignment(Pos.CENTER);
        Label icon = new Label("\uD83D\uDCC4");
        icon.getStyleClass().add("lease-doc-icon");
        Label title = new Label("Lease Agreement");
        title.getStyleClass().add("card-title");
        Label subtitle = new Label("No document on file");
        subtitle.getStyleClass().add("card-subtitle");
        content.getChildren().addAll(icon, title, subtitle);
        pane.getChildren().add(content);
        return pane;
    }

    private VBox buildStatsColumn(Lease lease) {
        VBox column = new VBox(16);
        column.setPrefWidth(250);
        column.getChildren().add(statCard("Start Date", formatDate(lease.GetStartDate())));
        column.getChildren().add(statCard("End Date", formatDate(lease.GetEndDate())));
        column.getChildren().add(statCard("Status", lease.GetStatus() == null ? "N/A" : lease.GetStatus().toUpperCase()));
        column.getChildren().add(statCard("Monthly Rent", String.format("\u20b1%,.2f", lease.GetMonthlyRent())));
        column.getChildren().add(statCard("Balance", String.format("\u20b1%,.2f", lease.GetBalance())));
        return column;
    }

    private VBox statCard(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("lease-stat-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("lease-stat-value");
        VBox card = new VBox(4, label, value);
        card.getStyleClass().add("lease-stat-card");
        return card;
    }

    private static String formatDate(java.time.LocalDate date) {
        return date == null ? "N/A" : date.toString();
    }
}
