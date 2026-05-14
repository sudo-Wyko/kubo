package com.teamroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("MySQL JDBC driver not found.", e);
                }

                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream("config.properties")) {
                    props.load(in);
                } catch (IOException e) {
                    try (InputStream in = ConnectionManager.class.getResourceAsStream("/config.properties")) {
                        if (in == null) {
                            throw new RuntimeException("config.properties not found.", e);
                        }
                        props.load(in);
                    } catch (IOException ex) {
                        throw new RuntimeException("config.properties not found.", ex);
                    }
                }

                String host = props.getProperty("db.host");
                String dbName = props.getProperty("db.name");
                String user = props.getProperty("db.user");
                String pass = props.getProperty("db.password");

                if (host == null || dbName == null || user == null || pass == null) {
                    throw new RuntimeException("Database configuration is incomplete in config.properties.");
                }

                String fullUrl = host + "/" + dbName + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

                // Set connection properties for UTF-8 encoding
                Properties connProps = new Properties();
                connProps.setProperty("user", user);
                connProps.setProperty("password", pass);
                connProps.setProperty("useUnicode", "true");
                connProps.setProperty("characterEncoding", "UTF-8");

                try {
                    connection = DriverManager.getConnection(fullUrl, connProps);
                    LOGGER.info(() -> "Connected to database: " + dbName);
                } catch (SQLException e) {
                    if (e.getErrorCode() == 1049) {
                        LOGGER.info(() -> "Database not found, initializing: " + dbName);
                        DatabaseInitializer.setupDatabase(host, dbName, user, pass);
                        connection = DriverManager.getConnection(fullUrl, connProps);
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
