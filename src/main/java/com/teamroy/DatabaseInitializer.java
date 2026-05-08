package com.teamroy;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
public class DatabaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    public static void setupDatabase(String host, String dbName, String user, String pass) {
        LOGGER.info(() -> "Database '" + dbName + "' not found. Initializing setup...");
        try (Connection serverConn = DriverManager.getConnection(host, user, pass);
             Statement serverStmt = serverConn.createStatement()) {
            serverStmt.executeUpdate("CREATE DATABASE " + dbName);
            LOGGER.info(() -> "Database created successfully: " + dbName);
        } catch (Exception e) {
            System.err.println("Failed to create database. Is MySQL running?");
            LOGGER.log(Level.SEVERE, "Failed to create database: " + dbName, e);
            return;
        }
        String newDbUrl = host + "/" + dbName + "?allowMultiQueries=true";
        try (Connection dbConn = DriverManager.getConnection(newDbUrl, user, pass);
             Statement dbStmt = dbConn.createStatement()) {
            String sqlScript = new String(Files.readAllBytes(Paths.get("database/schema.sql")));
            sqlScript = sqlScript.replace("DELIMITER    //", "");
            sqlScript = sqlScript.replace("DELIMITER ;", "");
            sqlScript = sqlScript.replace("//", ";");
            dbStmt.execute(sqlScript);
            LOGGER.info("Tables, triggers, and events created successfully.");
            LOGGER.info("Creating universal Super Admin account...");
            String defaultAdminPassword = "admin123";
            String hashedAdminPass = PasswordUtil.hash(defaultAdminPassword);
            String deleteAdmin = "DELETE FROM USER_ACCOUNT WHERE username = 'admin'";
            try (Statement deleteStmt = dbConn.createStatement()) {
                deleteStmt.executeUpdate(deleteAdmin);
            }
            String insertAdmin = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, 'ADMIN')";
            try (PreparedStatement stmt = dbConn.prepareStatement(insertAdmin)) {
                stmt.setString(1, "admin");
                stmt.setString(2, hashedAdminPass);
                int rowsInserted = stmt.executeUpdate();
                if (rowsInserted > 0) {
                    LOGGER.info("Super Admin account created successfully. Default credentials: admin / admin123");
                }
            } catch (SQLException e) {
                System.err.println("Failed to create admin account.");
                LOGGER.log(Level.SEVERE, "Failed to create admin account.", e);
            }
        } catch (Exception e) {
            System.err.println("Failed to execute schema.sql.");
            LOGGER.log(Level.SEVERE, "Failed to execute schema.sql.", e);
        }
    }
}
