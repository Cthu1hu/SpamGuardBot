package com.github.SpamGuardBot.config;

import java.sql.*;
import java.util.HashSet;
import java.util.Set;

public class UserDatabaseManager {
    private final String dbUrl = "jdbc:sqlite:excluded_users.db";

    public UserDatabaseManager() {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS excluded_users (" +
                    "user_id INTEGER PRIMARY KEY);";
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(createTableSQL);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addExcludedUser(long userId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String insertSQL = "INSERT OR IGNORE INTO excluded_users (user_id) VALUES (?);";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setLong(1, userId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isUserExcluded(long userId) {
        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            String selectSQL = "SELECT user_id FROM excluded_users WHERE user_id = ?;";
            try (PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
                pstmt.setLong(1, userId);
                ResultSet rs = pstmt.executeQuery();
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
