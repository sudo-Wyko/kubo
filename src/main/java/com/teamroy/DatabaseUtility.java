package com.teamroy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtility {
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    public static void initialize(String url, String username, String password) {
        URL = url;
        USER = username;
        PASSWORD = password;
    }

    public static Connection getConnection() {
        if (URL == null || USER == null || PASSWORD == null) {
            System.err.println("DatabaseUtility has not been initialized with credentials!");
            return null;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Connection Error: " + e.getMessage());
            return null;
        }
    }

    public static void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
