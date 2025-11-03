package com.mycompany.javafxapplication1;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.io.IOException;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Button registerBtn;
    @FXML private Button backLoginBtn;

    private final MySQLDB mysqlDatabase = new MySQLDB();
    private final DB sqliteDatabase = new DB();

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("standard", "admin");
        roleComboBox.setValue("standard");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirmPassword = confirmPasswordField.getText().trim();
        String role = roleComboBox.getValue();

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showDialog("Validation Error", "All fields are required!", Alert.AlertType.ERROR);
            return;
        }

        if (!password.equals(confirmPassword)) {
            showDialog("Password Error", "Passwords do not match!", Alert.AlertType.ERROR);
            return;
        }

        if (mysqlDatabase.userExists(username) || sqliteDatabase.userExists(username)) {
            showDialog("Error", "User already exists!", Alert.AlertType.ERROR);
            return;
        }

        boolean mysqlSuccess = mysqlDatabase.addUser(username, password, role);
        boolean sqliteSuccess = sqliteDatabase.addUser(username, password, role);

        if (mysqlSuccess && sqliteSuccess) {
            showDialog("Registration Successful", "User registered successfully!", Alert.AlertType.INFORMATION);
        } else if (mysqlSuccess) {
            showDialog("Warning", "User stored in MySQL but failed in SQLite.", Alert.AlertType.WARNING);
        } else if (sqliteSuccess) {
            showDialog("Warning", "User stored in SQLite but failed in MySQL.", Alert.AlertType.WARNING);
        } else {
            showDialog("Error", "User registration failed!", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) backLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root, 640, 480));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            showDialog("Error", "Failed to load login page!", Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void showDialog(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
