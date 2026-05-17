package com.teamroy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public final class DatabaseConfig {
    public static final Path CONFIG_PATH = Paths.get("config.properties");
    public static final String KEY_HOST = "db.host";
    public static final String KEY_NAME = "db.name";
    public static final String KEY_USER = "db.user";
    public static final String KEY_PASSWORD = "db.password";
    public static final String DEFAULT_HOST = "jdbc:mysql://localhost:3306";
    public static final String DEFAULT_USER = "root";

    private DatabaseConfig() {
    }

    public static boolean exists() {
        return Files.isRegularFile(CONFIG_PATH);
    }

    public static Properties load() throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
            props.load(in);
        }
        return props;
    }

    public static void save(String host, String dbName, String user, String password) throws IOException {
        Properties props = new Properties();
        props.setProperty(KEY_HOST, host);
        props.setProperty(KEY_NAME, dbName);
        props.setProperty(KEY_USER, user);
        props.setProperty(KEY_PASSWORD, password == null ? "" : password);
        try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Kubo database configuration");
        }
    }

    public static String buildJdbcUrl(String host, String dbName) {
        return host + "/" + dbName
                + "?useUnicode=true&characterEncoding=UTF-8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    public static Properties connectionProperties(String user, String password) {
        Properties connProps = new Properties();
        connProps.setProperty("user", user);
        connProps.setProperty("password", password == null ? "" : password);
        connProps.setProperty("useUnicode", "true");
        connProps.setProperty("characterEncoding", "UTF-8");
        connProps.setProperty("useSSL", "false");
        connProps.setProperty("allowPublicKeyRetrieval", "true");
        return connProps;
    }
}
