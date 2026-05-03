package com.teamroy.controller;

import com.teamroy.CurrencyUtil;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.LeaseDaoImpl;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.RoomDaoImpl;
import com.teamroy.model.entity.Lease;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Room;
import com.teamroy.model.entity.Tenant;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class TenantRoomsController {

    @FXML
    private Label emptyLabel;
    @FXML
    private VBox roomCard;
    @FXML
    private Label roomNumberLabel;
    @FXML
    private Label roomTypeBadge;
    @FXML
    private Label occupancyLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private ListView<String> maintList;

    private Connection getConnection() throws Exception {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("config.properties")) {
            props.load(in);
        }
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }

    @FXML
    private void initialize() {
        Tenant tenant = SessionManager.getCurrentTenant();
        if (tenant == null) {
            showEmpty("No tenant session found.");
            return;
        }

        try (Connection conn = getConnection()) {
            LeaseDaoImpl leaseDao = new LeaseDaoImpl(conn);
            RoomDaoImpl roomDao = new RoomDaoImpl(conn);
            MaintenanceRequestDaoImpl maintenanceDao = new MaintenanceRequestDaoImpl(conn);

            List<Lease> leases = leaseDao.GetByTenantId(tenant.GetTenantID());
            Lease lease = leases.stream()
                    .filter(l -> l != null && "ACTIVE".equalsIgnoreCase(l.GetStatus()))
                    .findFirst()
                    .orElse(null);

            if (lease == null) {
                showEmpty("No room assigned yet. Contact your administrator.");
                return;
            }
            Room room = roomDao.GetByID(lease.GetRoomID());
            if (room == null) {
                showEmpty("No room assigned yet. Contact your administrator.");
                return;
            }

            emptyLabel.setVisible(false);
            emptyLabel.setManaged(false);
            roomCard.setVisible(true);
            roomCard.setManaged(true);

            roomNumberLabel.setText("Room " + room.GetRoomNumber());
            roomTypeBadge.setText(room.GetRoomType());
            occupancyLabel.setText(room.GetCurrentOccupancy() + " / " + room.GetCapacity() + " beds occupied");
            priceLabel.setText(CurrencyUtil.format(room.GetPrice()) + " / month");

            List<MaintenanceRequest> open = maintenanceDao.GetByRoomID(room.GetRoomID()).stream()
                    .filter(m -> !"RESOLVED".equalsIgnoreCase(m.GetStatus()))
                    .collect(Collectors.toList());

            List<String> rows = open.stream()
                    .map(m -> "#" + m.GetRequestID() + " • "
                            + (m.GetReportedDate() == null ? "—" : m.GetReportedDate().toLocalDate()) + " • "
                            + m.GetStatus() + " — " + shorten(m.GetReportDescription()))
                    .collect(Collectors.toList());

            maintList.setItems(FXCollections.observableArrayList(rows));
        } catch (Exception ex) {
            ex.printStackTrace();
            showEmpty("Could not load room information.");
        }
    }

    private void showEmpty(String message) {
        emptyLabel.setText(message);
        emptyLabel.setVisible(true);
        emptyLabel.setManaged(true);
        roomCard.setVisible(false);
        roomCard.setManaged(false);
    }

    private static String shorten(String desc) {
        if (desc == null) {
            return "";
        }
        String t = desc.replace('\n', ' ').trim();
        return t.length() > 140 ? t.substring(0, 137) + "..." : t;
    }
}
