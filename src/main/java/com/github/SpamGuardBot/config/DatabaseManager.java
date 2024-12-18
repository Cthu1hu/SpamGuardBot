package com.github.SpamGuardBot.config;

import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:users.db";

    // Метод для инициализации базы данных
    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS incorrect_users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL UNIQUE);";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Таблица 'incorrect_users' проверена или создана.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Метод для добавления пользователя в базу данных
    public static void addUser(long userId) {
        String insertSQL = "INSERT OR IGNORE INTO incorrect_users (user_id) VALUES (?);";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setLong(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Метод для проверки, находится ли пользователь в базе данных
    public static boolean isUserInDatabase(long userId) {
        String selectSQL = "SELECT 1 FROM incorrect_users WHERE user_id = ? LIMIT 1;";

        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(selectSQL)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Метод для получения всех пользователей из базы данных
    public static void getAllUsers() {
        String selectSQL = "SELECT * FROM incorrect_users;";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            while (rs.next()) {
                long userId = rs.getLong("user_id");
                System.out.println("User ID: " + userId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
