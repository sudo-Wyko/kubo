package com.teamroy;
import java.io.FileInputStream;
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
    private ConnectionManager() {}
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream("config.properties")) {
                    props.load(in);
                } catch (IOException e) {
                    throw new RuntimeException("config.properties not found.", e);
                }
                String host = props.getProperty("db.host");
                String dbName = props.getProperty("db.name");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");
                String fullUrl = host + "/" + dbName;
                try {
                    connection = DriverManager.getConnection(fullUrl, user, pass);
                    LOGGER.info(() -> "Connected to database: " + dbName);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1049) {
                        LOGGER.info(() -> "Database not found, initializing: " + dbName);
                        DatabaseInitializer.setupDatabase(host, dbName, user, pass);
                        connection = DriverManager.getConnection(fullUrl, user, pass);
                        LOGGER.info(() -> "Connected to newly created database: " + dbName);
                    } else {
                        throw e;
                    }
                }
            }
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to connect to database.", e);
        }
    }
    public static void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to close database connection.", e);
        }
    }
}
