package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LogManager {
    public static void logAction(int userId, String action) {
        DB database = new DB(); 
        
        try (Connection conn = database.connect()) {
            String query = "INSERT INTO logs (user_id, action) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.executeUpdate();
            System.out.println("Log recorded: " + action);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
