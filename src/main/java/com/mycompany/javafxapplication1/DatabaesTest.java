package com.mycompany.javafxapplication1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaesTest {

    // MySQL Configuration
    private static final String DATABASE_NAME = "cwk_db";
    private static final String MYSQL_USER = "admin";
    private static final String MYSQL_PASS = "nour123";
    private static final String MYSQL_URL = "jdbc:mysql://host.docker.internal:3306/cwk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    // SQLite Configuration (Change path as necessary)
private static final String SQLITE_URL = "jdbc:sqlite:/home/ntu-user/NetBeansProjects/javaaanour1/cwk (1)/cwk/JavaFXApplication1/comp20081.db";


    public static void main(String[] args) {
        System.out.println("=== Testing MySQL Connection ===");
        testDatabaseConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASS, "MySQL");

        System.out.println("\n=== Testing SQLite Connection ===");
        testDatabaseConnection(SQLITE_URL, "", "", "SQLite");
    }

    private static void testDatabaseConnection(String dbUrl, String user, String password, String dbType) {
        try (Connection conn = (user.isEmpty()) ?
                DriverManager.getConnection(dbUrl) :
                DriverManager.getConnection(dbUrl, user, password)) {

            if (conn != null) {
                System.out.println("Connected to " + dbType + " successfully.");

                // Perform database operations
                addUser(conn, "testuser", "testpass", "standard", dbType);
                listUsers(conn, dbType);
            } else {
                System.err.println("Connection to " + dbType + " failed.");
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception in " + dbType + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addUser(Connection conn, String username, String password, String role, String dbType) {
        if (userExists(conn, username, dbType)) {
            System.out.println("User already exists in " + dbType + ": " + username);
            return;
        }

        String sql = dbType.equals("MySQL") ?
                "INSERT INTO Users (username, password_hash, role) VALUES (?, SHA2(?, 256), ?)" :
                "INSERT INTO Users (username, password_hash, role) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);

            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("User added successfully to " + dbType);
            } else {
                System.out.println("Failed to add user to " + dbType);
            }
        } catch (SQLException e) {
            System.err.println("Error inserting user into " + dbType + ": " + e.getMessage());
        }
    }

    private static boolean userExists(Connection conn, String username, String dbType) {
        String sql = "SELECT COUNT(*) FROM Users WHERE username = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error checking user existence in " + dbType + ": " + e.getMessage());
        }
        return false;
    }

    private static void listUsers(Connection conn, String dbType) {
        String sql = "SELECT id, username FROM Users";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            System.out.println("Users in " + dbType + " database:");
            while (rs.next()) {
                System.out.println("  ID: " + rs.getInt("id") + ", Username: " + rs.getString("username"));
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving users from " + dbType + ": " + e.getMessage());
        }
    }
}