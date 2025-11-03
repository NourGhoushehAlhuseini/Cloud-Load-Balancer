package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AdminController {

    @FXML private Button updatePasswordButton;
    @FXML private Button deleteUserButton;
    @FXML private Button logoutButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField newPasswordField;

    private final DB sqliteDatabase = new DB();
    private final MySQLDB mysqlDatabase = new MySQLDB();

    @FXML
    private void initialize() {
        boolean isAdmin = SessionManager.isAdmin();
        updatePasswordButton.setDisable(!isAdmin);
        deleteUserButton.setDisable(!isAdmin);
        if (!isAdmin) {
            showAlert("Access Denied", "Only admins can access these features.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleUpdatePassword() {
        String username = usernameField.getText().trim();
        String newPassword = newPasswordField.getText().trim();

        if (username.isEmpty() || newPassword.isEmpty()) {
            showAlert("Error", "Username and new password are required!", Alert.AlertType.ERROR);
            return;
        }

        boolean successSQLite = updatePasswordSQLite(username, newPassword);
        boolean successMySQL = updatePasswordMySQL(username, newPassword);

        if (successSQLite && successMySQL) {
            showAlert("Success", "Password updated successfully!", Alert.AlertType.INFORMATION);
        } else if (successSQLite) {
            showAlert("Warning", "Password updated in SQLite but failed in MySQL.", Alert.AlertType.WARNING);
        } else if (successMySQL) {
            showAlert("Warning", "Password updated in MySQL but failed in SQLite.", Alert.AlertType.WARNING);
        } else {
            showAlert("Error", "Failed to update password. Check database logs.", Alert.AlertType.ERROR);
        }
    }

    private boolean updatePasswordSQLite(String username, String newPassword) {
        try {
            boolean success = sqliteDatabase.updateUserPassword(username, newPassword);
            System.out.println("SQLite Password Update for " + username + ": " + success);
            return success;
        } catch (Exception e) {
            System.err.println("SQLite Password Update Error: " + e.getMessage());
            return false;
        }
    }

    private boolean updatePasswordMySQL(String username, String newPassword) {
        try {
            boolean success = mysqlDatabase.updateUserPassword(username, newPassword);
            System.out.println("MySQL Password Update for " + username + ": " + success);
            return success;
        } catch (Exception e) {
            System.err.println("MySQL Password Update Error: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleDeleteUser() {
        String username = usernameField.getText().trim();

        if (username.isEmpty()) {
            showAlert("Error", "Please enter a username!", Alert.AlertType.ERROR);
            return;
        }

        boolean successSQLite = deleteUserSQLite(username);
        boolean successMySQL = deleteUserMySQL(username);

        if (successSQLite && successMySQL) {
            showAlert("Success", "User deleted successfully!", Alert.AlertType.INFORMATION);
        } else if (successSQLite) {
            showAlert("Warning", "User deleted in SQLite but not in MySQL.", Alert.AlertType.WARNING);
        } else if (successMySQL) {
            showAlert("Warning", "User deleted in MySQL but not in SQLite.", Alert.AlertType.WARNING);
        } else {
            showAlert("Error", "Failed to delete user. Check database logs.", Alert.AlertType.ERROR);
        }
    }

    private boolean deleteUserSQLite(String username) {
        try {
            boolean success = sqliteDatabase.deleteUser(username);
            System.out.println("SQLite User Deletion for " + username + ": " + success);
            return success;
        } catch (Exception e) {
            System.err.println("SQLite User Deletion Error: " + e.getMessage());
            return false;
        }
    }

    private boolean deleteUserMySQL(String username) {
        try {
            boolean success = mysqlDatabase.deleteUser(username);
            System.out.println("MySQL User Deletion for " + username + ": " + success);
            return success;
        } catch (Exception e) {
            System.err.println("MySQL User Deletion Error: " + e.getMessage());
            return false;
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Failed to log out!", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
