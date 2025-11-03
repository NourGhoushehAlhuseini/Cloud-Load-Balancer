package com.mycompany.javafxapplication1;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class PrimaryController {

    @FXML private TextField userTextField;
    @FXML private PasswordField passPasswordField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;
    @FXML private Button adminPanelButton;

    private final DB sqliteDatabase = new DB();
    private final MySQLDB mysqlDatabase = new MySQLDB();

    @FXML
    private void initialize() {
        boolean isAdmin = SessionManager.isAdmin();
        adminPanelButton.setVisible(isAdmin);
        adminPanelButton.setManaged(isAdmin);
        System.out.println("Admin Panel Button Visible: " + isAdmin);
    }

    private void checkUserPermissions() {
        boolean isAdmin = SessionManager.isAdmin();
        adminPanelButton.setVisible(isAdmin);
        adminPanelButton.setManaged(isAdmin);
        System.out.println("Admin Panel Button Visible: " + isAdmin);
    }

    @FXML
    private void switchToSecondary() {
        String username = userTextField.getText().trim();
        String password = passPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Login Error", "Username and Password cannot be empty!", Alert.AlertType.ERROR);
            return;
        }

        boolean mysqlValid = mysqlDatabase.validateUser(username, password);
        boolean sqliteValid = sqliteDatabase.validateUser(username, password);

        if (mysqlValid || sqliteValid) {
            int userId = fetchUserId(username);
            String role = fetchUserRole(username);

            if (userId != -1) {
                SessionManager.setCurrentUser(username, userId, role);
                checkUserPermissions();
                switchScene("secondary.fxml", "Users List", userTextField);
            } else {
                showAlert("Login Error", "User ID not found.", Alert.AlertType.ERROR);
            }
        } else {
            showAlert("Login Failed", "Username or Password is incorrect. Please try again!", Alert.AlertType.ERROR);
        }
    }

    private void switchScene(String fxmlFile, String title, TextField textField) {
        Stage stage = (Stage) textField.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            stage.setScene(scene);
            stage.setTitle(title);
            stage.show();
        } catch (Exception e) {
            showAlert("Error", "Scene Loading Failed", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String contentMsg, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(contentMsg);
        alert.showAndWait();
    }

    private int fetchUserId(String username) {
        String query = "SELECT id FROM users WHERE username = ?";
        try (java.sql.Connection conn = sqliteDatabase.connect();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching user ID: " + e.getMessage());
        }
        return -1;
    }

    private String fetchUserRole(String username) {
        String query = "SELECT role FROM users WHERE username = ?";
        try (java.sql.Connection conn = sqliteDatabase.connect();
             java.sql.PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            java.sql.ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching user role: " + e.getMessage());
        }
        return "standard";
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Validation Error", "All fields are required!", Alert.AlertType.ERROR);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Password Error", "Passwords do not match!", Alert.AlertType.ERROR);
            return;
        }

        if (mysqlDatabase.userExists(username) || sqliteDatabase.userExists(username)) {
            showAlert("Error", "User already exists!", Alert.AlertType.ERROR);
            return;
        }

        boolean mysqlSuccess = mysqlDatabase.addUser(username, password, "standard");
        boolean sqliteSuccess = sqliteDatabase.addUser(username, password, "standard");

        if (mysqlSuccess && sqliteSuccess) {
            showAlert("Registration Successful", "User registered successfully in both databases!", Alert.AlertType.INFORMATION);
        } else if (mysqlSuccess) {
            showAlert("Warning", "User registered in MySQL but failed in SQLite.", Alert.AlertType.WARNING);
        } else if (sqliteSuccess) {
            showAlert("Warning", "User registered in SQLite but failed in MySQL.", Alert.AlertType.WARNING);
        } else {
            showAlert("Error", "User registration failed in both databases!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void switchToRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("register.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) registerButton.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Register");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void openAdminPanel() {
        if (SessionManager.isAdmin()) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("admin.fxml"));
                Parent root = loader.load();
                Stage stage = new Stage();
                stage.setScene(new Scene(root, 640, 480));
                stage.setTitle("Admin Panel");
                stage.show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            showAlert("Access Denied", "Only admins can access this feature.", Alert.AlertType.ERROR);
        }
    }
}
