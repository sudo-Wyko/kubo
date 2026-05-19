package com.teamroy;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static Connection connection;

    private ConnectionManager() {
        // Prevent instantiation
    }

    /**
     * Retrieves the global static database connection. 
     * Synchronized to prevent thread race conditions during initial setup or reconnection.
     */
    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                ensureDriverLoaded();
                Properties props = loadConfigurationProperties();

                String host = props.getProperty(DatabaseConfig.KEY_HOST);
                String dbName = props.getProperty(DatabaseConfig.KEY_NAME);
                String user = props.getProperty(DatabaseConfig.KEY_USER);
                String pass = props.getProperty(DatabaseConfig.KEY_PASSWORD, "");

                if (host == null || dbName == null || user == null) {
                    throw new RuntimeException(
                            "Database configuration is incomplete in " + DatabaseConfig.CONFIG_PATH + ".");
                }

                String fullUrl = DatabaseConfig.buildJdbcUrl(host, dbName);
                Properties connProps = DatabaseConfig.connectionProperties(user, pass);

                try {
                    connection = DriverManager.getConnection(fullUrl, connProps);
                    LOGGER.info(() -> "Connected to database: " + dbName);
                } catch (SQLException e) {
                    // MySQL Error 1049: Unknown Database
                    if (e.getErrorCode() == 1049) {
                        LOGGER.info(() -> "Database not found, initializing: " + dbName);
                        DatabaseInitializer.setupDatabase(host, dbName, user, pass);
                        
                        // Retry connection after initialization
                        connection = DriverManager.getConnection(fullUrl, connProps);
                        LOGGER.info(() -> "Connected to newly created database: " + dbName);
                    } else {
                        throw e;
                    }
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect or maintain database connection.", e);
        }
    }

    private static void ensureDriverLoaded() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found.", e);
        }
    }

    private static Properties loadConfigurationProperties() {
        if (!DatabaseConfig.exists()) {
            if (!DatabaseSetupDialog.promptUntilConfigured()) {
                throw new RuntimeException("Database setup was cancelled by the user.");
            }
        }
        try {
            return DatabaseConfig.load();
        } catch (IOException e) {
            throw new RuntimeException("Could not read " + DatabaseConfig.CONFIG_PATH + ".", e);
        }
    }

    /**
     * Safely closes the database connection and resets the internal reference.
     */
    public static synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed successfully.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to close database connection cleanly.", e);
        } finally {
            connection = null; // Always nullify to allow garbage collection and clean state
        }
    }
}