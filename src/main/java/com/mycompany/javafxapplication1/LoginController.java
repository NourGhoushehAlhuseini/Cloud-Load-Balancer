package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final DB sqliteDatabase = new DB();
    private final MySQLDB mysqlDatabase = new MySQLDB();

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showDialog("Login Error", "Please enter both username and password!", Alert.AlertType.ERROR);
            return;
        }

        boolean validSQLite = validateUserSQLite(username, password);
        boolean validMySQL = validateUserMySQL(username, password);

        if (validSQLite || validMySQL) {
            int userId = fetchUserId(username);
            String role = fetchUserRole(username);

            if (userId != -1) {
                SessionManager.setCurrentUser(username, userId, role);
                showDialog("Login Successful", "Welcome, " + username + "!", Alert.AlertType.INFORMATION);
                if (SessionManager.isAdmin()) {
                    switchToScene("admin.fxml", "Admin Panel");
                } else {
                    switchToScene("secondary.fxml", "File Management");
                }
            } else {
                showDialog("Login Error", "User ID not found.", Alert.AlertType.ERROR);
            }
        } else {
            showDialog("Login Failed", "Invalid username or password!", Alert.AlertType.ERROR);
        }
    }

    private boolean validateUserSQLite(String username, String password) {
        String query = "SELECT password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = sqliteDatabase.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return hashPassword(password, rs.getString("salt")).equals(rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            logError("SQLite error during login", e);
        }
        return false;
    }

    private boolean validateUserMySQL(String username, String password) {
        String query = "SELECT password_hash, salt FROM users WHERE username = ?";
        try (Connection conn = mysqlDatabase.getMySQLConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return hashPassword(password, rs.getString("salt")).equals(rs.getString("password_hash"));
            }
        } catch (SQLException e) {
            logError("MySQL error during login", e);
        }
        return false;
    }

    private int fetchUserId(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = sqliteDatabase.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            logError("Error fetching user ID", e);
        }
        return -1;
    }

    private String fetchUserRole(String username) {
        String query = "SELECT role FROM users WHERE username = ?";
        try (Connection conn = sqliteDatabase.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (SQLException e) {
            logError("Error fetching user role", e);
        }
        return "standard";
    }

    private void switchToScene(String fxmlFile, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            showDialog("Error", "Failed to switch scene: " + title, Alert.AlertType.ERROR);
            logError("Failed to load " + fxmlFile, e);
        }
    }

    private String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedBytes = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    private void showDialog(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void logError(String message, Exception e) {
        System.err.println(message + ": " + e.getMessage());
        e.printStackTrace();
    }
}
