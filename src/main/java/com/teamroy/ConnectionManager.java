package com.teamroy;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
public final class ConnectionManager {
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class.getName());
    private static Connection connection;
    /** True once we confirmed LEASE.charged_rent_periods exists (or added it) for this JVM. */
    private static boolean leaseChargedRentPeriodsEnsured;
    /** False after {@link #close()} so a new connection re-applies trigger removal if needed. */
    private static boolean paymentBalanceTriggerRemovalEnsured;
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
                ensureLeaseChargedRentPeriodsColumn(connection);
                removeObsoletePaymentBalanceTrigger(connection);
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
        } finally {
            leaseChargedRentPeriodsEnsured = false;
            paymentBalanceTriggerRemovalEnsured = false;
        }
    }
    /**
     * Older installs (e.g. from {@code init_kubo_db.sql} before charged accrual) are missing
     * {@code LEASE.charged_rent_periods}, which breaks lease INSERTs. Add the column once if needed.
     */
    private static void ensureLeaseChargedRentPeriodsColumn(Connection conn) {
        if (leaseChargedRentPeriodsEnsured || conn == null) {
            return;
        }
        synchronized (ConnectionManager.class) {
            if (leaseChargedRentPeriodsEnsured) {
                return;
            }
            try {
                boolean missing;
                try (Statement st = conn.createStatement();
                        ResultSet rs = st.executeQuery(
                                "SELECT COUNT(*) AS cnt FROM information_schema.COLUMNS "
                                        + "WHERE TABLE_SCHEMA = DATABASE() AND LOWER(TABLE_NAME) = 'lease' "
                                        + "AND COLUMN_NAME = 'charged_rent_periods'")) {
                    missing = !rs.next() || rs.getInt(1) == 0;
                }
                if (missing) {
                    try (Statement st = conn.createStatement()) {
                        st.executeUpdate(
                                "ALTER TABLE LEASE ADD COLUMN charged_rent_periods INT NOT NULL DEFAULT 0");
                        LOGGER.info("Applied DB fix: added LEASE.charged_rent_periods (required for rent accrual and new leases).");
                    }
                }
            } catch (SQLException e) {
                if (e.getErrorCode() == 1060) {
                    LOGGER.fine("LEASE.charged_rent_periods already exists.");
                } else {
                    LOGGER.log(Level.SEVERE,
                            "Could not ensure LEASE.charged_rent_periods exists; lease creation may fail until you run: "
                                    + "ALTER TABLE LEASE ADD COLUMN charged_rent_periods INT NOT NULL DEFAULT 0",
                            e);
                }
            } finally {
                leaseChargedRentPeriodsEnsured = true;
            }
        }
    }
    /**
     * Removes legacy {@code AFTER INSERT} trigger that subtracted balance for every payment status.
     * Balance is updated only in {@link com.teamroy.service.PaymentService}.
     */
    private static void removeObsoletePaymentBalanceTrigger(Connection conn) {
        if (paymentBalanceTriggerRemovalEnsured || conn == null) {
            return;
        }
        synchronized (ConnectionManager.class) {
            if (paymentBalanceTriggerRemovalEnsured) {
                return;
            }
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DROP TRIGGER IF EXISTS update_tenant_balance_after_payment");
                LOGGER.info("Dropped obsolete trigger update_tenant_balance_after_payment (tenant balance from payments is application-managed).");
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Could not DROP TRIGGER update_tenant_balance_after_payment", e);
            } finally {
                paymentBalanceTriggerRemovalEnsured = true;
            }
        }
    }
}
