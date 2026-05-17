package com.teamroy.controller;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.sql.*;
import java.util.List;
import java.time.format.DateTimeFormatter;
import com.teamroy.ConnectionManager;
import com.teamroy.SessionManager;
import com.teamroy.model.dao.MaintenanceRequestDaoImpl;
import com.teamroy.model.dao.AnnouncementDaoImpl;
import com.teamroy.model.dao.ActivityLogDaoImpl;
import com.teamroy.model.entity.MaintenanceRequest;
import com.teamroy.model.entity.Announcement;
import com.teamroy.model.entity.ActivityLog;
import javafx.scene.control.Button;
import javafx.scene.Cursor;
public class TenantHomeController {
    @FXML
    private Label lblWelcome;
    @FXML
    private Label lblRentAmount;
    @FXML
    private VBox maintenancePreviewList;
    @FXML
    private VBox announcementsList;
    @FXML
    private VBox activityList;
    @FXML
    private Button btnPayNow;
    private Connection conn = ConnectionManager.getConnection();
    private MaintenanceRequestDaoImpl maintenanceDao = new MaintenanceRequestDaoImpl(conn);
    private AnnouncementDaoImpl announcementDao = new AnnouncementDaoImpl(conn);
    private ActivityLogDaoImpl activityLogDao = new ActivityLogDaoImpl(conn);
    private int currentTenantId;
    @FXML
    public void initialize() {
        currentTenantId = SessionManager.getCurrentTenantId();
        System.out.println("TENANT ID: " + currentTenantId);
        if (conn != null) {
            loadDashboardData(currentTenantId);
        } else {
            System.err.println("Database connection is null in TenantHomeController.");
            lblWelcome.setText("Connection Error");
        }
        if (btnPayNow != null) {
            btnPayNow.setOnAction(e -> {
                TenantController.getInstance().LoadPaymentsView();
            });
        }
        maintenancePreviewList.setCursor(Cursor.HAND); 
        maintenancePreviewList.setOnMouseClicked(e -> {
            TenantController.getInstance().LoadMaintenanceView();
        });
    }
    private void loadDashboardData(int tenantId) {
        String tenantSql = "SELECT t.first_name, COALESCE(SUM(l.balance), 0) AS total_balance "
                + "FROM TENANT t LEFT JOIN LEASE l ON t.tenant_id = l.tenant_id WHERE t.tenant_id = ? "
                + "GROUP BY t.tenant_id, t.first_name";
        try (PreparedStatement ps = conn.prepareStatement(tenantSql)) {
            ps.setInt(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("first_name");
                    double balance = rs.getDouble("total_balance");
                    lblWelcome.setText("Welcome, " + (name != null ? name : "Tenant"));
                    lblRentAmount.setText(String.format("\u20b1 %,.2f", balance));
                }
            }
        } catch (SQLException e) {
            System.err.println("Could not fetch tenant details.");
            e.printStackTrace();
        }
        List<MaintenanceRequest> requests = maintenanceDao.GetByTenantID(tenantId);
        maintenancePreviewList.getChildren().clear();
        if (requests.isEmpty()) {
            Label noReqs = new Label("No recent requests.");
            noReqs.getStyleClass().add("card-subtitle");
            maintenancePreviewList.getChildren().add(noReqs);
        } else {
            int limit = Math.min(requests.size(), 3);
            for (int i = 0; i < limit; i++) {
                MaintenanceRequest req = requests.get(i);
                String title = req.GetReportDescription() != null ? req.GetReportDescription() : "Issue";
                if (title.length() > 25)
                    title = title.substring(0, 25) + "...";
                String status = req.GetStatus() != null ? req.GetStatus() : "UNKNOWN";
                HBox row = createMiniMaintenanceRow(title, status);
                maintenancePreviewList.getChildren().add(row);
            }
        }
        List<Announcement> announcements = announcementDao.GetRecentAnnouncements(5); 
        announcementsList.getChildren().clear();
        if (announcements.isEmpty()) {
            Label noAnn = new Label("No new announcements.");
            noAnn.getStyleClass().add("card-subtitle");
            announcementsList.getChildren().add(noAnn);
        } else {
            for (Announcement a : announcements) {
                announcementsList.getChildren().add(createAnnouncementRow(a));
            }
        }
        List<ActivityLog> logs = activityLogDao.GetRecentByTenantID(tenantId, 5); 
        activityList.getChildren().clear();
        if (logs.isEmpty()) {
            Label noAct = new Label("No recent activity.");
            noAct.getStyleClass().add("card-subtitle");
            activityList.getChildren().add(noAct);
        } else {
            for (ActivityLog log : logs) {
                activityList.getChildren().add(createActivityRow(log));
            }
        }
    }
    private HBox createMiniMaintenanceRow(String issue, String status) {
        HBox row = new HBox(10);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label lblIssue = new Label(issue);
        lblIssue.getStyleClass().add("row-description");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label lblStatus = new Label(status);
        lblStatus.getStyleClass().add("status-badge");
        String color = getStatusColor(status);
        lblStatus.setStyle("-fx-background-color: " + color + "22; -fx-text-fill: " + color
                + "; -fx-border-color: " + color + ";");
        row.getChildren().addAll(lblIssue, spacer, lblStatus);
        return row;
    }
    private VBox createAnnouncementRow(Announcement a) {
        VBox row = new VBox(3);
        Label lblTitle = new Label(a.GetTitle());
        lblTitle.getStyleClass().add("announcement-title");
        String dateStr = a.GetDatePosted() != null
                ? a.GetDatePosted().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                : "";
        Label lblDate = new Label(dateStr);
        lblDate.getStyleClass().add("card-subtitle");
        Label lblMessage = new Label(a.GetMessage());
        lblMessage.getStyleClass().add("card-subtitle");
        lblMessage.setWrapText(true);
        row.getChildren().addAll(lblTitle, lblDate, lblMessage);
        return row;
    }
    private VBox createActivityRow(ActivityLog log) {
        VBox row = new VBox(2);
        Label lblDesc = new Label(log.GetDescription());
        lblDesc.getStyleClass().addAll("card-subtitle", "row-description");
        lblDesc.setWrapText(true);
        String dateStr = log.GetCreatedAt() != null
                ? log.GetCreatedAt().format(DateTimeFormatter.ofPattern("MMM dd, hh:mm a"))
                : "";
        Label lblDate = new Label(dateStr);
        lblDate.getStyleClass().add("card-subtitle");
        row.getChildren().addAll(lblDesc, lblDate);
        return row;
    }
    private String getStatusColor(String status) {
        switch (status.toUpperCase()) {
            case "NEW":
                return "#f59e0b";
            case "IN-PROGRESS":
                return "#3b82f6";
            case "RESOLVED":
                return "#22c55e";
            default:
                return "#cbd5e1";
        }
    }
}
