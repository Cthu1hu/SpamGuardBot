package com.github.SpamGuardBot.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:users.db";

    // Метод для инициализации базы данных
    public static void initializeDatabase() {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS incorrect_users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER NOT NULL UNIQUE);";

        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            // Создание таблицы
            stmt.execute(createTableSQL);
            System.out.println("Таблица 'incorrect_users' проверена или создана.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
