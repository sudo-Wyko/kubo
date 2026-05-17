package com.teamroy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DatabaseInitializer {
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    private static final Path SCHEMA_FILE = Paths.get("database", "kubo.sql");

    private DatabaseInitializer() {
    }

    public static void setupDatabase(String host, String dbName, String user, String pass) {
        LOGGER.info(() -> "Initializing database '" + dbName + "'...");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found.", e);
        }

        Properties connProps = DatabaseConfig.connectionProperties(user, pass);
        try (Connection serverConn = DriverManager.getConnection(host, connProps);
             Statement serverStmt = serverConn.createStatement()) {
            serverStmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + sanitizeIdentifier(dbName) + "`");
            LOGGER.info(() -> "Database ready: " + dbName);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database. Is MySQL running?", e);
        }

        String newDbUrl = DatabaseConfig.buildJdbcUrl(host, dbName);
        try (Connection dbConn = DriverManager.getConnection(newDbUrl, connProps)) {
            executeSchemaScript(dbConn);
            createDefaultAdmin(dbConn);
            LOGGER.info("Tables, triggers, and events created successfully.");
        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to execute database/kubo.sql.", e);
        }
    }

    private static void executeSchemaScript(Connection dbConn) throws SQLException, IOException {
        String sqlScript = loadSchemaSql();

        sqlScript = sqlScript.replaceAll("(?i)DELIMITER\\s+//[\\r\\n]+", "");
        sqlScript = sqlScript.replaceAll("(?i)DELIMITER\\s+;[\\r\\n]+", "");
        sqlScript = sqlScript.replaceAll("(?i)END\\s+//", "END;");

        try (Statement dbStmt = dbConn.createStatement()) {
            dbStmt.executeUpdate("SET FOREIGN_KEY_CHECKS=0");

            StringBuilder currentStatement = new StringBuilder();
            int beginCount = 0;

            String[] lines = sqlScript.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                String trimmedUpper = trimmed.toUpperCase();

                if (trimmedUpper.startsWith("--")) {
                    continue;
                }

                if (trimmedUpper.matches(".*\\bBEGIN\\b.*")) {
                    beginCount++;
                }

                currentStatement.append(line).append("\n");

                if (trimmedUpper.matches("^END\\s*;\\s*$")) {
                    beginCount = Math.max(0, beginCount - 1);
                }

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
    }

    private static String loadSchemaSql() throws IOException {
        if (Files.isRegularFile(SCHEMA_FILE)) {
            return Files.readString(SCHEMA_FILE);
        }
        try (InputStream in = DatabaseInitializer.class.getResourceAsStream("/database/kubo.sql")) {
            if (in != null) {
                return new String(in.readAllBytes());
            }
        }
        throw new IOException("Schema file not found at " + SCHEMA_FILE.toAbsolutePath());
    }

    private static void createDefaultAdmin(Connection dbConn) throws SQLException {
        LOGGER.info("Creating universal Super Admin account...");
        String hashedAdminPass = PasswordUtil.hash("admin123");
        try (Statement deleteStmt = dbConn.createStatement()) {
            deleteStmt.executeUpdate("DELETE FROM USER_ACCOUNT WHERE username = 'admin'");
        }
        String insertAdmin = "INSERT INTO USER_ACCOUNT (username, password_hash, role) VALUES (?, ?, 'ADMIN')";
        try (PreparedStatement stmt = dbConn.prepareStatement(insertAdmin)) {
            stmt.setString(1, "admin");
            stmt.setString(2, hashedAdminPass);
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                LOGGER.info("Super Admin account created successfully. Default credentials: admin / admin123");
            }
        }
    }

    private static String sanitizeIdentifier(String identifier) {
        return identifier.replace("`", "");
    }
}
