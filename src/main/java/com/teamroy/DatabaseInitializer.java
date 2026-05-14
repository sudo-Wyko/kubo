package com.teamroy;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
public class DatabaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    public static void setupDatabase(String host, String dbName, String user, String pass) {
        LOGGER.info(() -> "Database '" + dbName + "' not found. Initializing setup...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found.", e);
        }
        Properties connProps = new Properties();
        connProps.setProperty("user", user);
        connProps.setProperty("password", pass);
        connProps.setProperty("useSSL", "false");
        connProps.setProperty("allowPublicKeyRetrieval", "true");
        try (Connection serverConn = DriverManager.getConnection(host, connProps);
             Statement serverStmt = serverConn.createStatement()) {
            serverStmt.executeUpdate("CREATE DATABASE " + dbName);
            LOGGER.info(() -> "Database created successfully: " + dbName);
        } catch (Exception e) {
            System.err.println("Failed to create database. Is MySQL running?");
            LOGGER.log(Level.SEVERE, "Failed to create database: " + dbName, e);
            return;
        }
        String newDbUrl = host + "/" + dbName + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        connProps.setProperty("useUnicode", "true");
        connProps.setProperty("characterEncoding", "UTF-8");
        try (Connection dbConn = DriverManager.getConnection(newDbUrl, connProps)) {
            String sqlScript = new String(Files.readAllBytes(Paths.get("database/schema.sql")));
            
            // Remove DELIMITER statements
            sqlScript = sqlScript.replaceAll("(?i)DELIMITER\\s+//[\\r\\n]+", "");
            sqlScript = sqlScript.replaceAll("(?i)DELIMITER\\s+;[\\r\\n]+", "");
            
            // Replace END // with END; (trigger/event terminators only)
            sqlScript = sqlScript.replaceAll("(?i)END\\s+//", "END;");
            
            try (Statement dbStmt = dbConn.createStatement()) {
                dbStmt.executeUpdate("SET FOREIGN_KEY_CHECKS=0");
                
                // Parse statements respecting BEGIN...END blocks
                StringBuilder currentStatement = new StringBuilder();
                int beginCount = 0;
                
                String[] lines = sqlScript.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String trimmed = line.trim();
                    String trimmedUpper = trimmed.toUpperCase();
                    
                    // Skip comment-only lines
                    if (trimmedUpper.startsWith("--")) {
                        continue;
                    }
                    
                    // Count BEGIN keywords
                    if (trimmedUpper.matches(".*\\bBEGIN\\b.*")) {
                        beginCount++;
                    }
                    
                    currentStatement.append(line).append("\n");
                    
                    // Only count standalone END; (not END IF; or END WHILE; etc)
                    if (trimmedUpper.matches("^END\\s*;\\s*$")) {
                        beginCount = Math.max(0, beginCount - 1);
                    }
                    
                    // Split on semicolon only if we're at top level (beginCount == 0) and line ends with ;
                    if (trimmed.endsWith(";") && beginCount == 0 && !trimmed.isEmpty()) {
                        String stmt = currentStatement.toString().trim();
                        
                        if (!stmt.isEmpty() && !stmt.equals(";")) {
                            try {
                                dbStmt.execute(stmt);
                            } catch (SQLException e) {
                                String preview = stmt.replaceAll("\\s+", " ");
                                if (preview.length() > 100) {
                                    preview = preview.substring(0, 100) + "...";
                                }
                                LOGGER.log(Level.WARNING, "Statement failed (continuing): " + preview, e);
                            }
                        }
                        currentStatement = new StringBuilder();
                    }
                }
                
                dbStmt.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
            }
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
